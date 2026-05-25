import { Component, effect, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RestaurantService, Restaurant, Table } from '../../core/services/restaurant.service';
import { ImageService } from '../../core/services/image.service';
import QRCode from 'qrcode';

const THEMES = [
  { id: 'classique', label: 'Classique' },
  { id: 'chaud', label: 'Chaud' },
  { id: 'nature', label: 'Nature' },
  { id: 'elegant', label: 'Elegant' }
] as const;

@Component({
  selector: 'app-settings',
  imports: [ReactiveFormsModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss'
})
export class SettingsComponent {
  private image = inject(ImageService);
  restaurant = inject(RestaurantService);

  themes = THEMES;
  loading = false;
  saved = false;
  qrCodes = signal<Map<string, string>>(new Map());
  showQrPanel = false;

  form = new FormGroup({
    name: new FormControl('', Validators.required),
    address: new FormControl(''),
    themeId: new FormControl('classique', Validators.required),
    paymentProviderAccountId: new FormControl(''),
  });

  private syncRestaurantState = effect(() => {
    const restaurant = this.restaurant.restaurant();
    if (!restaurant) {
      return;
    }
    this.form.patchValue({
      name: restaurant.name,
      address: restaurant.address ?? '',
      themeId: restaurant.themeId,
      paymentProviderAccountId: restaurant.paymentProviderAccountId ?? '',
    }, { emitEvent: false });
  });

  save(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.saved = false;

    this.restaurant.updateRestaurant({
      name: this.form.controls.name.value ?? '',
      address: this.form.controls.address.value ?? '',
      themeId: this.form.controls.themeId.value ?? 'classique',
      paymentProviderAccountId: this.form.controls.paymentProviderAccountId.value ?? '',
    }).subscribe({
      next: () => {
        this.loading = false;
        this.saved = true;
        setTimeout(() => this.saved = false, 3000);
      },
      error: (err) => {
        alert(err.error?.message ?? err.error?.error ?? 'Erreur');
        this.loading = false;
      }
    });
  }

  onLogoUpload(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    this.image.upload('logos', file).subscribe({
      next: (res) => {
        this.restaurant.updateRestaurant({ logoPath: res.url }).subscribe({
          error: (err) => alert(err.error?.message ?? err.error?.error ?? 'Erreur lors de la mise à jour du logo'),
        });
      },
      error: (err) => alert(err.error?.message ?? err.error?.error ?? "Erreur lors de l'upload")
    });
  }

  generateQrCodes(): void {
    const restaurant = this.restaurant.restaurant();
    const tables = this.restaurant.tables();

    if (!restaurant) {
      alert('Restaurant introuvable');
      return;
    }

    if (tables.length === 0) {
      this.restaurant.loadTables().subscribe({
        next: (loadedTables) => this.doGenerateQr(restaurant.clientBaseUrl, restaurant.slug, loadedTables),
        error: (err) => alert(err.error?.message ?? err.error?.error ?? 'Impossible de charger les tables'),
      });
      return;
    }

    this.doGenerateQr(restaurant.clientBaseUrl, restaurant.slug, tables);
  }

  private doGenerateQr(clientBaseUrl: string, slug: string, tables: Table[]): void {
    const map = new Map<string, string>();
    const normalizedClientBaseUrl = this.normalizeBaseUrl(clientBaseUrl);

    Promise.all(
      tables.map(t =>
        QRCode.toDataURL(`${normalizedClientBaseUrl}/menu/${slug}/${t.id}`, { width: 256, margin: 2 })
          .then(url => [t.id, url, t.number] as const)
      )
    ).then(results => {
      for (const [id, url] of results) {
        map.set(id, url);
      }
      this.qrCodes.set(map);
      this.showQrPanel = true;
    });
  }

  private normalizeBaseUrl(url: string): string {
    if (url.endsWith('/')) {
      return url.slice(0, -1);
    }
    return url;
  }

  getTableName(tableId: string): number {
    return this.restaurant.tables().find(t => t.id === tableId)?.number ?? 0;
  }

  downloadQr(tableId: string): void {
    const url = this.qrCodes().get(tableId);
    if (!url) return;

    const a = document.createElement('a');
    a.href = url;
    a.download = `table-${this.getTableName(tableId)}-qr.png`;
    a.click();
  }
}
