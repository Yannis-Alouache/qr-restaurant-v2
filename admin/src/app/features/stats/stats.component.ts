import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, of } from 'rxjs';
import { Observable } from 'rxjs';
import { RestaurantService } from '../../core/services/restaurant.service';
import {
  StatsService,
  RestaurantStats,
  MonthlyStats,
  MIN_MONTHS,
  MAX_MONTHS,
} from '../../core/services/stats.service';
import {
  LucideBanknote,
  LucideClipboardList,
  LucideChartColumn,
  LucideTrendingUp,
  LucideArrowUpRight,
  LucideArrowDownRight,
  LucideMinus,
  LucideCalendar,
  LucideChevronDown,
  LucideBookOpen,
  LucidePrinter,
  LucideRotateCw,
  LucideCircleAlert,
} from '../../core/icons';

interface DeltaVm {
  dir: 'up' | 'down' | 'flat';
  label: string;
}

interface KpiFootVm {
  delta: DeltaVm;
  cmp: string;
}

interface KpisVm {
  revenue: { value: string; foot: KpiFootVm };
  orders: { value: string; foot: KpiFootVm };
  average: { value: string; foot: KpiFootVm };
  refunds: { value: string; sub: string; foot: KpiFootVm };
}

interface ChartGridVm {
  y: string;
  leftLabel: string;
  rightLabel: string;
}

interface ChartColumnVm {
  cursorX: string;
  cursorW: string;
  bar: { x: string; y: string; w: string; h: string; rx: string } | null;
  hitX: string;
  hitW: string;
  hitAria: string;
  label: { x: string; text: string } | null;
}

interface ChartVm {
  grid: ChartGridVm[];
  columns: ChartColumnVm[];
  linePoints: string;
  dots: { cx: string; cy: string; r: string; last: boolean }[];
  hasData: boolean;
}

interface ChartInfoVm {
  month: string;
  orders: string;
  revenue: string;
  average: string | null;
}

interface TopItemVm {
  rank: string;
  name: string;
  qtyLabel: string;
  ca: string;
  barWidth: string;
}

/** Échelle « gentille » pour les axes : 7 → 10, 23 → 40, 48 → 60. */
function niceCeil(v: number): number {
  const p = Math.pow(10, Math.floor(Math.log10(v)));
  const u = v / p;
  return (u <= 1 ? 1 : u <= 2 ? 2 : u <= 4 ? 4 : u <= 6 ? 6 : 10) * p;
}

@Component({
  selector: 'app-stats',
  imports: [
    RouterLink,
    LucideBanknote,
    LucideClipboardList,
    LucideChartColumn,
    LucideTrendingUp,
    LucideArrowUpRight,
    LucideArrowDownRight,
    LucideMinus,
    LucideCalendar,
    LucideChevronDown,
    LucideBookOpen,
    LucidePrinter,
    LucideRotateCw,
    LucideCircleAlert,
  ],
  templateUrl: './stats.component.html',
  styleUrl: './stats.component.scss',
})
export class StatsComponent implements OnInit {
  private readonly statsService = inject(StatsService);
  private readonly restaurantService = inject(RestaurantService);
  private readonly destroyRef = inject(DestroyRef);

  readonly minMonths = MIN_MONTHS;
  readonly maxMonths = MAX_MONTHS;
  readonly monthOptions = Array.from({ length: MAX_MONTHS }, (_, i) => i + 1);

  readonly months = this.statsService.selectedMonths;
  readonly restaurant = this.restaurantService.restaurant;

  readonly loading = signal(true);
  readonly loadError = signal(false);
  private readonly current = signal<RestaurantStats | null>(null);
  private readonly extended = signal<RestaurantStats | null>(null);
  private readonly hoverIndex = signal<number | null>(null);

  /** Format fr-FR, identiques à la maquette validée. */
  private readonly eur = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' });
  private readonly eur0 = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 });
  private readonly int = new Intl.NumberFormat('fr-FR');
  private readonly pct = new Intl.NumberFormat('fr-FR', { style: 'percent', maximumFractionDigits: 1 });
  private readonly fmtShort = new Intl.DateTimeFormat('fr-FR', { month: 'short', year: '2-digit' });
  private readonly fmtLong = new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' });

  private readonly rows = computed<MonthlyStats[]>(() => this.current()?.monthly ?? []);

  /** Restaurant encore sans commande payée : page d'accueil « état vide ». */
  readonly hasNoOrders = computed(() => {
    const summary = this.current()?.summary;
    return summary !== undefined && summary.totalOrders === 0;
  });

  readonly kpis = computed<KpisVm | null>(() => {
    const summary = this.current()?.summary;
    if (!summary) return null;
    const deltas = this.deltas();
    const foot = (delta: DeltaVm | null): KpiFootVm => ({
      delta: delta ?? { dir: 'flat', label: 'n/d' },
      cmp: delta === null ? 'historique indisponible' : this.previousCmp(),
    });
    const refundSub = summary.refundedOrders > 0
      ? `${this.int.format(summary.refundedOrders)} commande${summary.refundedOrders > 1 ? 's' : ''} remboursée${summary.refundedOrders > 1 ? 's' : ''}`
      : 'Aucun remboursement sur la période';
    return {
      revenue: { value: this.eur.format(summary.totalRevenue), foot: foot(deltas?.revenue ?? null) },
      orders: { value: this.int.format(summary.totalOrders), foot: foot(deltas?.orders ?? null) },
      average: {
        value: summary.totalOrders > 0 ? this.eur.format(summary.averageOrderValue) : 'n/d',
        foot: foot(deltas?.average ?? null),
      },
      refunds: { value: this.eur.format(summary.refundedAmount), sub: refundSub, foot: foot(deltas?.refunds ?? null) },
    };
  });

  readonly rangePill = computed(() => {
    const rows = this.rows();
    if (rows.length === 0) return '';
    const first = this.monthLabelLong(rows[0].month);
    const last = this.monthLabelLong(rows[rows.length - 1].month);
    return rows.length === 1 ? last : `${first} → ${last}`;
  });

  readonly lastCompletePill = computed(() => {
    const now = new Date();
    const lastComplete = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    return `Dernier mois complet : ${this.fmtLong.format(lastComplete).replace(/[\u202f\u00a0]/g, ' ')}`;
  });

  readonly chart = computed<ChartVm | null>(() => {
    const rows = this.rows();
    if (rows.length === 0) return null;
    const W = 960, H = 320, ML = 46, MR = 62, MT = 20, MB = 40;
    const pw = W - ML - MR;
    const ph = H - MT - MB;
    const n = rows.length;
    const maxC = niceCeil(Math.max(4, ...rows.map(m => m.orders)) * 1.05);
    const maxR = niceCeil(Math.max(200, ...rows.map(m => m.revenue)) * 1.05);
    const xc = (i: number) => ML + (i + 0.5) * pw / n;
    const yC = (v: number) => MT + ph - (v / maxC) * ph;
    const yR = (v: number) => MT + ph - (v / maxR) * ph;
    const bw = Math.min(34, (pw / n) * 0.5);

    const grid: ChartGridVm[] = [0, 0.25, 0.5, 0.75, 1].map(f => ({
      y: (MT + ph - f * ph).toFixed(1),
      leftLabel: this.int.format(Math.round(maxC * f)),
      rightLabel: this.eur0.format(maxR * f),
    }));

    const columns: ChartColumnVm[] = rows.map((m, i) => {
      const showLabel = n <= 12 || (n <= 20 && i % 2 === 0) || i % 3 === 0;
      return {
        cursorX: (ML + i * pw / n).toFixed(1),
        cursorW: (pw / n).toFixed(1),
        bar: m.orders > 0 ? {
          x: (xc(i) - bw / 2).toFixed(1),
          y: yC(m.orders).toFixed(1),
          w: bw.toFixed(1),
          h: (MT + ph - yC(m.orders)).toFixed(1),
          rx: Math.min(5, bw / 2).toFixed(1),
        } : null,
        hitX: (ML + i * pw / n).toFixed(1),
        hitW: (pw / n).toFixed(1),
        hitAria: `${this.monthLabelLong(m.month)} : ${m.orders} commande${m.orders > 1 ? 's' : ''}, ${this.eur.format(m.revenue)}`,
        label: showLabel ? { x: xc(i).toFixed(1), text: this.monthLabelShort(m.month) } : null,
      };
    });

    const dots = rows.map((m, i) => ({
      cx: xc(i).toFixed(1),
      cy: yR(m.revenue).toFixed(1),
      r: i === n - 1 ? '4.5' : '3',
      last: i === n - 1,
    }));

    return {
      grid,
      columns,
      linePoints: dots.map(d => `${d.cx},${d.cy}`).join(' '),
      dots,
      hasData: rows.some(m => m.orders > 0 || m.revenue > 0),
    };
  });

  readonly chartInfo = computed<ChartInfoVm | null>(() => {
    const rows = this.rows();
    if (rows.length === 0) return null;
    const index = Math.min(this.hoverIndex() ?? rows.length - 1, rows.length - 1);
    const m = rows[index];
    return {
      month: this.monthLabelLong(m.month),
      orders: `${this.int.format(m.orders)} commande${m.orders > 1 ? 's' : ''}`,
      revenue: this.eur.format(m.revenue),
      average: m.orders > 0 ? 'panier ' + this.eur.format(m.averageOrderValue) : null,
    };
  });

  readonly topItems = computed<TopItemVm[]>(() => {
    const items = this.current()?.topItems ?? [];
    const max = items.length > 0 ? items[0].revenue : 1;
    return items.map((it, i) => ({
      rank: String(i + 1).padStart(2, '0'),
      name: it.name,
      qtyLabel: `${this.int.format(it.quantitySold)} vendu${it.quantitySold > 1 ? 's' : ''}`,
      ca: this.eur.format(it.revenue),
      barWidth: (it.revenue / max * 100).toFixed(1) + '%',
    }));
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    const months = this.months();
    this.loading.set(true);
    this.loadError.set(false);
    this.hoverIndex.set(null);

    // La fenêtre doublée alimente les badges d'évolution « vs N mois précédents ».
    forkJoin({
      current: this.statsService.getStats(months),
      extended: this.extendedRequest(months),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ current, extended }) => {
          this.current.set(current);
          this.extended.set(extended);
          this.loading.set(false);
        },
        error: () => {
          this.current.set(null);
          this.extended.set(null);
          this.loadError.set(true);
          this.loading.set(false);
        },
      });
  }

  onMonthsChange(event: Event): void {
    const value = parseInt((event.target as HTMLSelectElement).value, 10);
    if (!Number.isNaN(value) && value !== this.months()) {
      this.statsService.setMonths(value);
      this.reload();
    }
  }

  onColumnHover(index: number): void {
    this.hoverIndex.set(index);
  }

  onChartLeave(): void {
    this.hoverIndex.set(null);
  }

  private extendedRequest(months: number): Observable<RestaurantStats | null> {
    return months * 2 <= MAX_MONTHS ? this.statsService.getStats(months * 2) : of(null);
  }

  /**
   * Évolution vs fenêtre précédente de même longueur : la requête doublée
   * donne le total 2N, la fenêtre courante N — la différence est le N précédent.
   */
  private deltas(): { revenue: DeltaVm | null; orders: DeltaVm | null; average: DeltaVm | null; refunds: DeltaVm | null } | null {
    const current = this.current();
    const extended = this.extended();
    if (!current) return null;
    if (!extended) return { revenue: null, orders: null, average: null, refunds: null };

    const summary = current.summary;
    const prevRevenue = extended.summary.totalRevenue - summary.totalRevenue;
    const prevOrders = extended.summary.totalOrders - summary.totalOrders;
    const prevAverage = prevOrders > 0 ? prevRevenue / prevOrders : 0;
    const prevRefunds = extended.summary.refundedAmount - summary.refundedAmount;
    return {
      revenue: this.ratio(summary.totalRevenue, prevRevenue),
      orders: this.ratio(summary.totalOrders, prevOrders),
      average: this.ratio(summary.averageOrderValue, prevAverage),
      refunds: this.ratio(summary.refundedAmount, prevRefunds),
    };
  }

  private ratio(current: number, previous: number): DeltaVm | null {
    if (!(previous > 0)) return null;
    const d = (current - previous) / previous;
    return { dir: d >= 0 ? 'up' : 'down', label: (d >= 0 ? '+' : '−') + this.pct.format(Math.abs(d)) };
  }

  private previousCmp(): string {
    const n = this.months();
    return `vs ${n} mois précédent${n > 1 ? 's' : ''}`;
  }

  private monthToDate(key: string): Date {
    const [year, month] = key.split('-').map(Number);
    return new Date(year, month - 1, 1);
  }

  private monthLabelShort(key: string): string {
    return this.fmtShort.format(this.monthToDate(key)).replace(/[\u202f\u00a0]/g, ' ');
  }

  private monthLabelLong(key: string): string {
    return this.fmtLong.format(this.monthToDate(key)).replace(/[\u202f\u00a0]/g, ' ');
  }
}
