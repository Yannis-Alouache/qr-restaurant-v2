import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ADMIN_ICONS } from '../../core/icons';
import { LEGAL_INFO } from './legal-info';

@Component({
  selector: 'app-privacy',
  imports: [RouterLink, ...ADMIN_ICONS],
  templateUrl: './privacy.component.html',
})
export class PrivacyComponent {
  protected readonly legal = LEGAL_INFO;
}
