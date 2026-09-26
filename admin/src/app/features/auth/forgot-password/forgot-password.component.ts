import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthShellComponent } from '../auth-shell.component';
import { ADMIN_ICONS } from '../../../core/icons';

@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, RouterLink, AuthShellComponent, ...ADMIN_ICONS],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss',
})
export class ForgotPasswordComponent {
  private auth = inject(AuthService);

  loading = signal(false);
  sent = signal(false);
  error = signal('');

  form = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email]),
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.auth.requestPasswordReset(this.form.controls.email.value as string).subscribe({
      next: () => {
        this.sent.set(true);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Une erreur est survenue, veuillez réessayer dans un instant.");
        this.loading.set(false);
      },
    });
  }
}
