import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ADMIN_ICONS } from '../../core/icons';

@Component({
  selector: 'app-terms',
  imports: [RouterLink, ...ADMIN_ICONS],
  templateUrl: './terms.component.html',
})
export class TermsComponent {}
