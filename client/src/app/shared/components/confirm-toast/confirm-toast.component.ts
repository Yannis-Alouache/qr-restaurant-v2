import { Component, signal } from '@angular/core';

/** Toast de confirmation en haut d'écran — copie pixel de la maquette. */
@Component({
  selector: 'app-confirm-toast',
  templateUrl: './confirm-toast.component.html',
  styleUrl: './confirm-toast.component.scss',
})
export class ConfirmToastComponent {
  readonly title = signal('');
  readonly detail = signal<string | null>(null);

  readonly visible = signal(false);
  private timer: ReturnType<typeof setTimeout> | null = null;

  show(title: string, detail: string | null = null): void {
    this.title.set(title);
    this.detail.set(detail);
    this.visible.set(true);
    if (this.timer) clearTimeout(this.timer);
    this.timer = setTimeout(() => this.visible.set(false), 3000);
  }
}
