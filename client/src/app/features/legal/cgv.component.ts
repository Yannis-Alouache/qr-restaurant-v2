import { Component, inject } from '@angular/core';
import { Location } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LEGAL_INFO } from './legal-info';

/** Conditions Générales de Vente côté application de commande (public client final). */
@Component({
  selector: 'app-legal-cgv',
  imports: [RouterLink],
  templateUrl: './cgv.component.html',
  styleUrl: './legal-page.scss',
})
export class CgvComponent {
  private readonly location = inject(Location);

  protected readonly legal = LEGAL_INFO;

  protected goBack(): void {
    this.location.back();
  }
}
