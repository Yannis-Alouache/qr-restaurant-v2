import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RestaurantService, Table } from '../../core/services/restaurant.service';
import { ImageService } from '../../core/services/image.service';
import { ToastService } from '../../core/services/toast.service';
import QRCode from 'qrcode';
import { ADMIN_ICONS } from '../../core/icons';

const THEMES = [
  { id: 'classique', name: 'Classique', sub: 'Noir et blanc', cls: 'theme-preview-classic' },
  { id: 'chaud', name: 'Chaud', sub: 'Tons orangés', cls: 'theme-preview-warm' },
  { id: 'nature', name: 'Nature', sub: 'Tons verts', cls: 'theme-preview-nature' },
  { id: 'elegant', name: 'Élégant', sub: 'Tons violets', cls: 'theme-preview-elegant' },
] as const;

interface QrEntry {
  id: string;
  number: number;
  url: string;
  link: string;
}

@Component({
  selector: 'app-settings',
  imports: [ReactiveFormsModule, ...ADMIN_ICONS],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent implements OnInit {
  private image = inject(ImageService);
  private toast = inject(ToastService);
  restaurant = inject(RestaurantService);

  themes = THEMES;
  loading = signal(false);
  saved = signal(false);
  uploadingLogo = signal(false);
  qrEntries = signal<QrEntry[]>([]);

  /** All fields preserved on save; only name/theme/logo are surfaced in the UI. */
  form = new FormGroup({
    name: new FormControl('', Validators.required),
    address: new FormControl(''),
    themeId: new FormControl('classique', Validators.required),
    paymentProviderAccountId: new FormControl(''),
  });

  constructor() {
    effect(() => {
      const restaurant = this.restaurant.restaurant();
      if (!restaurant) {
        return;
      }
      this.form.patchValue(
        {
          name: restaurant.name,
          address: restaurant.address ?? '',
          themeId: restaurant.themeId,
          paymentProviderAccountId: restaurant.paymentProviderAccountId ?? '',
        },
        { emitEvent: false },
      );
    });
  }

  ngOnInit(): void {
    this.generateQrCodes();
  }

  // ── Restaurant info ────────────────────────────────────────────────
  save(): void {
    if (this.form.invalid || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.saved.set(false);

    this.restaurant
      .updateRestaurant({
        name: this.form.controls.name.value ?? '',
        address: this.form.controls.address.value ?? '',
        themeId: this.form.controls.themeId.value ?? 'classique',
        paymentProviderAccountId: this.form.controls.paymentProviderAccountId.value ?? '',
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.saved.set(true);
          this.toast.show('Informations mises à jour');
          setTimeout(() => this.saved.set(false), 2500);
        },
        error: (err) => {
          this.toast.show(err.error?.message ?? err.error?.error ?? 'Erreur');
          this.loading.set(false);
        },
      });
  }

  // ── Logo ───────────────────────────────────────────────────────────
  onLogoUpload(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) {
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      this.toast.show('Le logo ne doit pas dépasser 2 Mo');
      return;
    }
    this.uploadingLogo.set(true);
    (event.target as HTMLInputElement).value = '';
    this.image.upload('logos', file).subscribe({
      next: (res) => {
        this.restaurant.updateRestaurant({ logoPath: res.url }).subscribe({
          next: () => {
            this.uploadingLogo.set(false);
            this.toast.show('Logo mis à jour');
          },
          error: () => {
            this.uploadingLogo.set(false);
            this.toast.show('Erreur lors de la mise à jour du logo');
          },
        });
      },
      error: () => {
        this.uploadingLogo.set(false);
        this.toast.show("Erreur lors de l'import du logo");
      },
    });
  }

  removeLogo(): void {
    this.restaurant.updateRestaurant({ logoPath: '' }).subscribe({
      next: () => this.toast.show('Logo supprimé'),
      error: () => this.toast.show('Erreur lors de la suppression'),
    });
  }

  // ── Theme ──────────────────────────────────────────────────────────
  selectTheme(id: string): void {
    this.form.controls.themeId.setValue(id);
    this.restaurant.updateRestaurant({ themeId: id }).subscribe({
      next: () => this.toast.show('Thème appliqué au menu client'),
      error: () => this.toast.show('Erreur lors du changement de thème'),
    });
  }

  // ── QR codes ───────────────────────────────────────────────────────
  tableCount(): number {
    return this.restaurant.tables().length || this.qrEntries().length;
  }

  generateQrCodes(): void {
    const restaurant = this.restaurant.restaurant();
    if (!restaurant) {
      // Retry once the restaurant has loaded.
      this.restaurant.loadRestaurant().subscribe({ next: () => this.generateQrCodes() });
      return;
    }

    const tables = this.restaurant.tables();
    if (tables.length === 0) {
      this.restaurant.loadTables().subscribe({
        next: (loaded) => this.buildQr(restaurant.clientBaseUrl, restaurant.slug, loaded),
        error: () => this.toast.show('Impossible de charger les tables'),
      });
      return;
    }
    this.buildQr(restaurant.clientBaseUrl, restaurant.slug, tables);
  }

  private buildQr(clientBaseUrl: string, slug: string, tables: Table[]): void {
    const base = this.normalizeBaseUrl(clientBaseUrl);
    Promise.all(
      tables.map((t) =>
        QRCode.toDataURL(`${base}/menu/${slug}/${t.id}`, { width: 256, margin: 2 }).then(
          (url) => ({ id: t.id, number: t.number, url, link: `${base}/menu/${slug}/${t.id}` }) as QrEntry,
        ),
      ),
    ).then((entries) => this.qrEntries.set(entries));
  }

  private normalizeBaseUrl(url: string): string {
    return url.endsWith('/') ? url.slice(0, -1) : url;
  }

  downloadQr(entry: QrEntry): void {
    const a = document.createElement('a');
    a.href = entry.url;
    a.download = `table-${entry.number}-qr.png`;
    a.click();
    this.toast.show(`QR Table ${entry.number} téléchargé`);
  }

  printQr(): void {
    window.print();
  }
}
