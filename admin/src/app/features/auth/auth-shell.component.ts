import { Component, ViewEncapsulation } from '@angular/core';

/**
 * Shared split layout for the three auth surfaces (login, signup, onboarding):
 * a dark brand panel on the left and a white form panel on the right.
 *
 * Each page projects its own brand illustration/tagline via [brandBody] and
 * its form via the default slot.
 *
 * `ViewEncapsulation.None` is intentional: the shell owns the visual styling
 * of the content projected into <ng-content>, which Emulated encapsulation
 * would scope away from those projected nodes (they belong to the projecting
 * component, not this one).
 */
@Component({
  selector: 'app-auth-shell',
  templateUrl: './auth-shell.component.html',
  styleUrl: './auth-shell.component.scss',
  encapsulation: ViewEncapsulation.None,
})
export class AuthShellComponent {}
