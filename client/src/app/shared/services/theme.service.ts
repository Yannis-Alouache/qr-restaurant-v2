import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

const STORAGE_KEY = 'qr-restaurant-theme';
const SUPPORTED_THEMES = new Set(['classique', 'chaud', 'nature', 'elegant']);

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);

  readonly currentTheme = signal<string>('classique');

  initialize(): void {
    const storedTheme =
      typeof localStorage !== 'undefined' ? localStorage.getItem(STORAGE_KEY) : null;

    this.apply(storedTheme);
  }

  apply(themeId: string | null | undefined): void {
    const theme = SUPPORTED_THEMES.has(themeId ?? '') ? themeId! : 'classique';

    this.document.documentElement.setAttribute('data-theme', theme);
    this.currentTheme.set(theme);

    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, theme);
    }
  }
}
