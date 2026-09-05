import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MenuService } from '../../services/menu.service';
import { ThemeService } from '../../../../shared/services/theme.service';

/**
 * Écran bloquant affiché quand le lien ne porte pas un UUID de table valide
 * (voir tableIdGuard). Volontairement sans aucune action : l'utilisateur ne
 * peut pas continuer, il doit scanner le QR code de sa table. Le menu du
 * restaurant est chargé uniquement pour appliquer le thème choisi par l'admin
 * (servi depuis le cache du MenuService s'il est déjà connu) ; si le slug est
 * inconnu, le thème courant est conservé tel quel.
 */
@Component({
  selector: 'app-invalid-qr-screen',
  imports: [],
  templateUrl: './invalid-qr-screen.component.html',
  styleUrl: './invalid-qr-screen.component.scss',
})
export class InvalidQrScreenComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly menuService = inject(MenuService);
  private readonly themeService = inject(ThemeService);

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug') ?? '';
    if (!slug) {
      return;
    }

    this.menuService.getMenu(slug).subscribe({
      next: menu => this.themeService.apply(menu.restaurant.themeId),
      error: () => undefined,
    });
  }
}
