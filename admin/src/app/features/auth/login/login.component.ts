import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { RestaurantService } from '../../../core/services/restaurant.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private auth = inject(AuthService);
  private restaurant = inject(RestaurantService);
  private router = inject(Router);

  error = '';
  loading = false;

  form = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [Validators.required])
  });

  submit(): void {
    if (this.form.invalid) return;

    this.loading = true;
    this.error = '';

    this.auth.login(this.form.value as { email: string; password: string }).subscribe({
      next: () => this.navigateAfterLogin(),
      error: (err) => {
        this.error = err.error?.error ?? 'Identifiants invalides';
        this.loading = false;
      }
    });
  }

  private navigateAfterLogin(): void {
    this.restaurant.loadRestaurant().subscribe({
      next: () => this.router.navigate(['/menu']),
      error: () => this.router.navigate(['/onboarding'])
    });
  }
}
