import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthShellComponent } from '../auth-shell.component';
import { ADMIN_ICONS } from '../../../core/icons';

/** Matches the API password policy: ≥8 chars, upper, lower, digit, special. */
const strongPasswordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;

@Component({
  selector: 'app-signup',
  imports: [ReactiveFormsModule, RouterLink, AuthShellComponent, ...ADMIN_ICONS],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.scss',
})
export class SignupComponent implements OnInit {
  private auth = inject(AuthService);
  private router = inject(Router);

  /** Fournisseurs activés côté API : le bouton Google n'est affiché que si configuré. */
  readonly googleEnabled = signal(false);

  error = signal('');
  loading = signal(false);
  showPassword = signal(false);
  showConfirm = signal(false);

  form = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(strongPasswordPattern),
    ]),
    confirmPassword: new FormControl('', [Validators.required]),
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

  toggleConfirm(): void {
    this.showConfirm.update(v => !v);
  }

  passwordsMatch(): boolean {
    const pw = this.form.controls.password.value;
    const confirm = this.form.controls.confirmPassword.value;
    return !confirm || pw === confirm;
  }

  submit(): void {
    if (this.form.invalid || !this.passwordsMatch()) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    const { email, password } = this.form.value as { email: string; password: string };
    this.auth.signup({ email, password }).subscribe({
      next: () => this.router.navigate(['/onboarding']),
      error: (err) => {
        this.error.set(err.error?.message ?? err.error?.error ?? "Erreur lors de l'inscription");
        this.loading.set(false);
      },
    });
  }
}
