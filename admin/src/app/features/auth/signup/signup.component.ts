import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

const strongPasswordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{10,}$/;

@Component({
  selector: 'app-signup',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.scss'
})
export class SignupComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  error = '';
  loading = false;

  form = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [
      Validators.required,
      Validators.minLength(10),
      Validators.pattern(strongPasswordPattern),
    ]),
  });

  submit(): void {
    if (this.form.invalid) return;

    this.loading = true;
    this.error = '';

    this.auth.signup(this.form.value as { email: string; password: string }).subscribe({
      next: () => this.router.navigate(['/onboarding']),
      error: (err) => {
        this.error = err.error?.error ?? 'Erreur lors de l\'inscription';
        this.loading = false;
      }
    });
  }
}
