import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { RestaurantService } from '../../core/services/restaurant.service';

const THEMES = [
  { id: 'classique', label: 'Classique' },
  { id: 'chaud', label: 'Chaud' },
  { id: 'nature', label: 'Nature' },
  { id: 'elegant', label: 'Elegant' }
] as const;

@Component({
  selector: 'app-onboarding',
  imports: [ReactiveFormsModule],
  templateUrl: './onboarding.component.html',
  styleUrl: './onboarding.component.scss'
})
export class OnboardingComponent {
  private restaurant = inject(RestaurantService);
  private router = inject(Router);

  themes = THEMES;
  error = '';
  loading = false;

  form = new FormGroup({
    name: new FormControl('', [Validators.required]),
    tableCount: new FormControl(5, [Validators.required, Validators.min(1), Validators.max(50)]),
    themeId: new FormControl('classique', [Validators.required])
  });

  submit(): void {
    if (this.form.invalid) return;

    this.loading = true;
    this.error = '';

    this.restaurant.createRestaurant(this.form.value as { name: string; tableCount: number; themeId: string }).subscribe({
      next: () => this.router.navigate(['/menu']),
      error: (err) => {
        this.error = err.error?.error ?? 'Erreur lors de la création';
        this.loading = false;
      }
    });
  }
}
