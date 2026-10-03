import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthShellComponent } from '../auth-shell.component';
import { ADMIN_ICONS } from '../../../core/icons';

/** Matches the API password policy: ≥8 chars, upper, lower, digit, special. */
const strongPasswordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink, AuthShellComponent, ...ADMIN_ICONS],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss',
})
export class ResetPasswordComponent {
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);

  /** Token carried by the email link; absent when the URL was truncated or already consumed. */
  token = this.route.snapshot.queryParamMap.get('token');

  /** 'form' (token ok), 'missing' (no token in URL), 'success' after a successful reset. */
  state = signal<'form' | 'missing' | 'success'>(this.token ? 'form' : 'missing');
  loading = signal(false);
  error = signal('');
  showPassword = signal(false);
  showConfirm = signal(false);

  form = new FormGroup({
    password: new FormControl('', [
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(strongPasswordPattern),
    ]),
    confirmPassword: new FormControl('', [Validators.required]),
  });

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
    if (!this.token || this.form.invalid || !this.passwordsMatch()) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.auth.resetPassword({
      token: this.token,
      newPassword: this.form.controls.password.value as string,
    }).subscribe({
      next: () => {
        this.state.set('success');
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? err.error?.error ?? 'Impossible de réinitialiser le mot de passe');
        this.loading.set(false);
      },
    });
  }
}
