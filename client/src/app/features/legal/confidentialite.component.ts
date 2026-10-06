import { Component, inject } from '@angular/core';
import { Location } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LEGAL_INFO } from './legal-info';

/** Politique de confidentialité côté application de commande (public client final). */
@Component({
  selector: 'app-legal-confidentialite',
  imports: [RouterLink],
  templateUrl: './confidentialite.component.html',
  styleUrl: './legal-page.scss',
})
export class ConfidentialiteComponent {
  private readonly location = inject(Location);

  protected readonly legal = LEGAL_INFO;

  protected goBack(): void {
    this.location.back();
  }
}
