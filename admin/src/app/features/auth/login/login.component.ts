import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { RestaurantService } from '../../../core/services/restaurant.service';
import { AuthShellComponent } from '../auth-shell.component';
import { ADMIN_ICONS } from '../../../core/icons';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, AuthShellComponent, ...ADMIN_ICONS],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private auth = inject(AuthService);
  private restaurant = inject(RestaurantService);
  private router = inject(Router);

  /** Retour d'échec de la connexion Google (redirection API) — via withComponentInputBinding. */
  readonly erreur = input<string>();
  /** Fournisseurs activés côté API : le bouton Google n'est affiché que si configuré. */
  readonly googleEnabled = signal(false);

  error = signal('');
  readonly googleError = computed(() =>
    this.erreur() === 'google'
      ? 'La connexion avec Google a échoué. Réessayez, ou connectez-vous avec votre adresse email.'
      : '',
  );
  readonly displayedError = computed(() => this.error() || this.googleError());

  loading = signal(false);
  showPassword = signal(false);

  form = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [Validators.required]),
  });

  ngOnInit(): void {
    this.auth.getProviders().subscribe({
      next: (providers) => this.googleEnabled.set(providers.google),
      error: () => this.googleEnabled.set(false),
    });
  }

  togglePassword(): void {
    this.showPassword.update(v => !v);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.auth.login(this.form.value as { email: string; password: string }).subscribe({
      next: () => this.navigateAfterLogin(),
      error: (err) => {
        this.error.set(err.error?.message ?? err.error?.error ?? 'Identifiants invalides');
        this.loading.set(false);
      },
    });
  }

  private navigateAfterLogin(): void {
    this.restaurant.loadRestaurant().subscribe({
      next: () => this.router.navigate(['/orders']),
      error: (err) => {
        if (err.status === 401 || err.status === 403) {
          this.auth.logout().subscribe(() => this.router.navigate(['/login']));
        } else {
          this.router.navigate(['/onboarding']);
        }
      },
    });
  }
}
