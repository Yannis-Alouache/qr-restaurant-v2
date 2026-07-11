import { Component, Input, forwardRef, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { LucideMinus, LucidePlus } from '@lucide/angular';

/**
 * Single normalized price field for the Menzo admin.
 *
 * Wraps the global `.number-input-group` primitive: a `[−] € value [+]` row
 * with click-to-step buttons (one `step` per click, clamped at `min`), a
 * centered tabular value, and a euro adornment. Used for every price input
 * (article price, menu price, formula supplements) so the look and behavior
 * are identical everywhere.
 *
 * Implements ControlValueAccessor so it drops into reactive forms via
 * `formControlName` / `[formControl]`. The model is a `number | null`; a local
 * string buffer backs the input so users can type decimals freely (a controlled
 * number input fights the user — "1." yields NaN and Angular resets the field).
 */
@Component({
  selector: 'app-price-input',
  imports: [LucideMinus, LucidePlus],
  templateUrl: './price-input.component.html',
  styleUrl: './price-input.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PriceInputComponent),
      multi: true,
    },
  ],
})
export class PriceInputComponent implements ControlValueAccessor {
  /** Increment size. 0.50 for article prices, 0.10 for formula supplements. */
  @Input() step = 0.5;
  /** Lower bound. Buttons never go below this. */
  @Input() min = 0;
  /** Shown only when the field is empty (model is null). */
  @Input() placeholder = '';
  /** Forwarded to the inner <input> so a <label for="…"> can target it. */
  @Input() inputId = '';

  /** Raw user text — the single source of truth for what's displayed. */
  readonly text = signal('');
  readonly disabled = signal(false);

  private onChange: (v: number | null) => void = () => {};
  private onTouched: () => void = () => {};

  // ── ControlValueAccessor ───────────────────────────────────────────
  writeValue(value: number | null): void {
    this.text.set(value == null ? '' : formatPrice(value));
  }
  registerOnChange(fn: (v: number | null) => void): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }
  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  // ── User interactions ──────────────────────────────────────────────
  onInput(event: Event): void {
    const raw = (event.target as HTMLInputElement).value;
    this.text.set(raw);
    this.onChange(parsePrice(raw));
  }

  markTouched(): void {
    this.onTouched();
  }

  inc(): void {
    this.bump(this.step);
  }
  dec(): void {
    this.bump(-this.step);
  }

  private bump(delta: number): void {
    const current = parsePrice(this.text()) ?? 0;
    const next = round2(Math.max(this.min, current + delta));
    this.text.set(formatPrice(next));
    this.onChange(next);
    this.onTouched();
  }
}

/** Parses a user-typed price ("1,5" or "1.5") into a number, or null if empty/invalid. */
function parsePrice(raw: string): number | null {
  if (!raw) {
    return null;
  }
  const n = Number.parseFloat(raw.trim().replace(',', '.'));
  return Number.isFinite(n) && n >= 0 ? n : null;
}

/** Rounds to 2 decimals (prices are in euro cents), avoiding float drift. */
function round2(n: number): number {
  return Math.round((n + Number.EPSILON) * 100) / 100;
}

/** Formats a number for display, French-style: 12.5 → "12,50". */
function formatPrice(n: number): string {
  return n.toFixed(2).replace('.', ',');
}
