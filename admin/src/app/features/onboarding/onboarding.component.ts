import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { RestaurantService } from '../../core/services/restaurant.service';
import { ImageService } from '../../core/services/image.service';
import { AuthShellComponent } from '../auth/auth-shell.component';
import { ADMIN_ICONS } from '../../core/icons';

const THEMES = [
  { id: 'classique', name: 'Classique', sub: 'Noir et blanc', cls: 'theme-preview-classic' },
  { id: 'chaud', name: 'Chaud', sub: 'Tons orangés', cls: 'theme-preview-warm' },
  { id: 'nature', name: 'Nature', sub: 'Tons verts', cls: 'theme-preview-nature' },
  { id: 'elegant', name: 'Élégant', sub: 'Tons violets', cls: 'theme-preview-elegant' },
] as const;

const MAX_LOGO_BYTES = 2 * 1024 * 1024;
const MIN_TABLES = 1;
const MAX_TABLES = 50;

@Component({
  selector: 'app-onboarding',
  imports: [ReactiveFormsModule, AuthShellComponent, ...ADMIN_ICONS],
  templateUrl: './onboarding.component.html',
  styleUrl: './onboarding.component.scss',
})
export class OnboardingComponent {
  private restaurant = inject(RestaurantService);
  private image = inject(ImageService);
  private router = inject(Router);

  themes = THEMES;
  error = signal('');
  loading = signal(false);

  logoFile = signal<File | null>(null);
  logoPreview = signal<string>('');
  dragOver = signal(false);

  form = new FormGroup({
    name: new FormControl('', [Validators.required]),
    tableCount: new FormControl(10, [Validators.required, Validators.min(MIN_TABLES), Validators.max(MAX_TABLES)]),
    themeId: new FormControl('classique', [Validators.required]),
  });

  increment(): void {
    const next = Math.min(MAX_TABLES, (this.form.controls.tableCount.value ?? MIN_TABLES) + 1);
    this.form.controls.tableCount.setValue(next);
  }

  decrement(): void {
    const next = Math.max(MIN_TABLES, (this.form.controls.tableCount.value ?? MIN_TABLES) - 1);
    this.form.controls.tableCount.setValue(next);
  }

  selectTheme(id: string): void {
    this.form.controls.themeId.setValue(id);
  }

  onLogoInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.setLogo(file);
    }
  }

  onLogoDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.setLogo(file);
    }
  }

  removeLogo(): void {
    this.logoFile.set(null);
    this.logoPreview.set('');
  }

  private setLogo(file: File): void {
    if (!file.type.startsWith('image/')) {
      this.error.set("Le fichier doit être une image.");
      return;
    }
    if (file.size > MAX_LOGO_BYTES) {
      this.error.set("Le logo ne doit pas dépasser 2 Mo.");
      return;
    }
    this.error.set('');
    this.logoFile.set(file);
    const reader = new FileReader();
    reader.onload = e => this.logoPreview.set(e.target?.result as string);
    reader.readAsDataURL(file);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    const { name, tableCount, themeId } = this.form.value as {
      name: string;
      tableCount: number;
      themeId: string;
    };

    const create = (logoPath?: string) => {
      this.restaurant.createRestaurant({ name, tableCount, themeId, logoPath }).subscribe({
        next: () => this.router.navigate(['/menu']),
        error: (err) => {
          this.error.set(err.error?.message ?? err.error?.error ?? 'Erreur lors de la création');
          this.loading.set(false);
        },
      });
    };

    const file = this.logoFile();
    if (file) {
      this.image.upload('logos', file).subscribe({
        next: (res) => create(res.url),
        error: () => {
          this.error.set("Erreur lors de l'import du logo");
          this.loading.set(false);
        },
      });
    } else {
      create();
    }
  }
}
