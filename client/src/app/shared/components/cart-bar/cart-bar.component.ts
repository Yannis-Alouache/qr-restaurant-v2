import { Component, inject, output } from '@angular/core';
import { CartService } from '../../../features/cart/services/cart.service';
import { PricePipe } from '../../pipes/price.pipe';

/** Barre de panier fixe en bas d'écran — copie pixel de la maquette. */
@Component({
  selector: 'app-cart-bar',
  templateUrl: './cart-bar.component.html',
  styleUrl: './cart-bar.component.scss',
  imports: [PricePipe],
})
export class CartBarComponent {
  readonly cart = inject(CartService);
  readonly openCart = output<void>();
}
