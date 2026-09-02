import { Component, computed, input } from '@angular/core';

/**
 * Bandeau d'en-tête façon maquette : photo pleine largeur, logo centré
 * au-dessus d'un dégradé vers la couleur de fond.
 * Le contenu projeté (bouton Retour) se superpose au bandeau.
 */
@Component({
  selector: 'app-restaurant-header',
  templateUrl: './restaurant-header.component.html',
  styleUrl: './restaurant-header.component.scss',
})
export class RestaurantHeaderComponent {
  /** Chemin de l'image de couverture (null → dégradé de repli). */
  coverPath = input<string | null>(null);
  /** Logo du restaurant (null → nom affiché en texte). */
  logoPath = input<string | null>(null);
  restaurantName = input.required<string>();
  /** Hauteur imposée en px ; laisser null pour le ratio 16/9 natif (max 200px). */
  heightPx = input<number | null>(null);

  hasCover = computed(() => !!this.coverPath());
  hasLogo = computed(() => !!this.logoPath());
}
