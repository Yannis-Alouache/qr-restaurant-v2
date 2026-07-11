import { Injectable, signal } from '@angular/core';

export type ConfirmTone = 'danger' | 'primary';

export interface ConfirmOptions {
  title: string;
  message: string;
  /** Defaults to "Confirmer". */
  confirmLabel?: string;
  /** Defaults to "Annuler". */
  cancelLabel?: string;
  /** Defaults to "primary" (blue). Use "danger" for destructive actions. */
  tone?: ConfirmTone;
}

export interface ConfirmState extends Required<ConfirmOptions> {}

/**
 * App-wide confirmation dialog. Rendered once (in the layout shell) so any
 * page can `await confirm.ask({...})` without hosting its own element — the
 * same pattern as ToastService. Replaces the native browser `confirm()`.
 */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly _state = signal<ConfirmState | null>(null);
  private resolver?: (value: boolean) => void;

  readonly state = this._state.asReadonly();

  /** Opens the dialog and resolves `true` on confirm, `false` on cancel. */
  ask(options: ConfirmOptions): Promise<boolean> {
    return new Promise<boolean>(resolve => {
      this.resolver = resolve;
      this._state.set({
        title: options.title,
        message: options.message,
        confirmLabel: options.confirmLabel ?? 'Confirmer',
        cancelLabel: options.cancelLabel ?? 'Annuler',
        tone: options.tone ?? 'primary',
      });
    });
  }

  confirm(): void {
    this.close(true);
  }

  cancel(): void {
    this.close(false);
  }

  private close(result: boolean): void {
    const resolve = this.resolver;
    this.resolver = undefined;
    this._state.set(null);
    resolve?.(result);
  }
}
