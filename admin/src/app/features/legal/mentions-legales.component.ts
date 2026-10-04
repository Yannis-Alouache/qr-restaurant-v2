import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ADMIN_ICONS } from '../../core/icons';
import { LEGAL_INFO, LEGAL_PENDING } from './legal-info';

@Component({
  selector: 'app-mentions-legales',
  imports: [RouterLink, ...ADMIN_ICONS],
  templateUrl: './mentions-legales.component.html',
})
export class MentionsLegalesComponent {
  protected readonly legal = LEGAL_INFO;
  protected readonly pending = LEGAL_PENDING;
}
