import { Component, inject } from '@angular/core';
import { Location } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LEGAL_INFO, LEGAL_PENDING } from './legal-info';

/** Mentions légales côté application de commande (public client final). */
@Component({
  selector: 'app-legal-mentions-legales',
  imports: [RouterLink],
  templateUrl: './mentions-legales.component.html',
  styleUrl: './legal-page.scss',
})
export class MentionsLegalesComponent {
  private readonly location = inject(Location);

  protected readonly legal = LEGAL_INFO;
  protected readonly pending = LEGAL_PENDING;

  protected goBack(): void {
    this.location.back();
  }
}
