import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;
  let document: Document;

  beforeEach(() => {
    localStorage.removeItem('qr-restaurant-theme');

    TestBed.configureTestingModule({});
    service = TestBed.inject(ThemeService);
    document = TestBed.inject(DOCUMENT);
  });

  afterEach(() => {
    localStorage.removeItem('qr-restaurant-theme');
    document.documentElement.removeAttribute('data-theme');
  });

  it('applies a supported theme to the document and persists it', () => {
    service.apply('chaud');

    expect(document.documentElement.getAttribute('data-theme')).toBe('chaud');
    expect(service.currentTheme()).toBe('chaud');
    expect(localStorage.getItem('qr-restaurant-theme')).toBe('chaud');
  });

  it('falls back to the default theme when an unknown theme is provided', () => {
    service.apply('unknown-theme');

    expect(document.documentElement.getAttribute('data-theme')).toBe('classique');
    expect(service.currentTheme()).toBe('classique');
  });
});
