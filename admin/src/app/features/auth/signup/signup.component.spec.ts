import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { SignupComponent } from './signup.component';

describe('SignupComponent', () => {
  it('keeps the form invalid until the password matches the strengthened policy', () => {
    TestBed.configureTestingModule({
      imports: [SignupComponent],
      providers: [
        {
          provide: AuthService,
          useValue: {
            signup: vi.fn().mockReturnValue(of({ token: 'token', userId: 'user-1' })),
          },
        },
        {
          provide: Router,
          useValue: {
            navigate: vi.fn(),
          },
        },
        {
          provide: ActivatedRoute,
          useValue: {},
        },
      ],
    });

    const fixture = TestBed.createComponent(SignupComponent);
    const component = fixture.componentInstance;

    component.form.setValue({
      email: 'owner@example.com',
      password: 'secret123',
    });
    component.form.get('password')?.markAsTouched();
    fixture.detectChanges();

    expect(component.form.invalid).toBe(true);
    expect(component.form.get('password')?.hasError('pattern')).toBe(true);
    expect(fixture.nativeElement.querySelector('[data-testid="signup-password-hint"]')?.textContent).toContain(
      '10 caractères minimum',
    );
  });
});
