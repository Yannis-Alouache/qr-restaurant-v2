import { Injectable, computed, signal } from '@angular/core';

/**
 * Tiny app-wide toast. Rendered once (in the navbar shell) so every page can
 * call `toast.show(...)` without hosting its own element.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly _message = signal('');
  private timer: ReturnType<typeof setTimeout> | null = null;

  readonly message = this._message.asReadonly();
  readonly visible = computed(() => this._message().length > 0);

  show(message: string, duration = 2400): void {
    this._message.set(message);
    if (this.timer) {
      clearTimeout(this.timer);
    }
    this.timer = setTimeout(() => this._message.set(''), duration);
  }
}
