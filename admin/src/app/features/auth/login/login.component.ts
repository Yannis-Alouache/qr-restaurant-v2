import { Component, inject, signal } from '@angular/core';
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
export class LoginComponent {
  private auth = inject(AuthService);
  private restaurant = inject(RestaurantService);
  private router = inject(Router);

  error = signal('');
  loading = signal(false);
  showPassword = signal(false);

  form = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [Validators.required]),
  });

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
