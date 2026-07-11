import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ADMIN_ICONS } from '../../core/icons';

@Component({
  selector: 'app-privacy',
  imports: [RouterLink, ...ADMIN_ICONS],
  templateUrl: './privacy.component.html',
})
export class PrivacyComponent {}
