import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { MenuService } from '../../../menu/services/menu.service';
import { CartService } from '../../../cart/services/cart.service';
import { CartEntry } from '../../../cart/models/cart.model';
import { ThemeService } from '../../../../shared/services/theme.service';
import { PricePipe } from '../../../../shared/pipes/price.pipe';
import { RestaurantHeaderComponent } from '../../../../shared/components/restaurant-header/restaurant-header.component';
import { categoryCover } from '../../../menu/utils/menu-utils';

type PayState = 'idle' | 'processing';

/** Écran 5 de la maquette : récapitulatif de commande puis paiement Stripe. */
@Component({
  selector: 'app-checkout-page',
  templateUrl: './checkout-page.component.html',
  styleUrl: './checkout-page.component.scss',
  imports: [PricePipe, RestaurantHeaderComponent],
})
export class CheckoutPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly orderService = inject(OrderService);
  private readonly menuService = inject(MenuService);
  private readonly themeService = inject(ThemeService);
  readonly cart = inject(CartService);

  restaurantName = signal<string>('');
  logoPath = signal<string | null>(null);
  coverPath = signal<string | null>(null);

  payState = signal<PayState>('idle');
  error = signal<string | null>(null);
  createdOrderId = signal<string | null>(null);

  protected readonly slug = this.route.snapshot.paramMap.get('slug') ?? '';
  protected readonly tableId = this.route.snapshot.paramMap.get('tableId') ?? '';

  readonly itemCountLabel = computed(() => {
    const count = this.cart.itemCount();
    return `${count} article${count > 1 ? 's' : ''}`;
  });

  ngOnInit(): void {
    if (this.slug) {
      // Le menu est en cache : simple rafraîchissement d'identité du restaurant.
      this.menuService.getMenu(this.slug).subscribe({
        next: menu => {
          this.themeService.apply(menu.restaurant.themeId);
          this.restaurantName.set(menu.restaurant.name);
          this.logoPath.set(menu.restaurant.logoPath);
          for (const category of menu.categories) {
            const cover = categoryCover(category);
            if (cover) {
              this.coverPath.set(cover);
              break;
            }
          }
        },
        error: () => {
          // Le récapitulatif reste utilisable sans l'identité du restaurant.
        },
      });
    }
  }

  entryName(entry: CartEntry): string {
    return entry.type === 'standalone' ? entry.name : entry.mainItem.name;
  }

  entryImg(entry: CartEntry): string | null {
    if (entry.type === 'standalone') return entry.imagePath;
    return entry.mainItem.imagePath;
  }

  /** Détail de composition affiché en italique sous le nom d'un menu. */
  entryMenuDetail(entry: CartEntry): string | null {
    if (entry.type !== 'combo') return null;
    return `${entry.sideItem.name} + ${entry.drinkItem.name}`;
  }

  entryUnitPrice(entry: CartEntry): number {
    if (entry.type === 'standalone') return entry.price;
    return entry.mainItem.price + entry.sideItem.supplementPrice + entry.drinkItem.supplementPrice;
  }

  pay(): void {
    if (this.payState() === 'processing') return;

    if (!this.slug || !this.tableId) {
      this.error.set('Informations manquantes');
      return;
    }
    if (this.cart.isEmpty()) {
      this.error.set('Votre panier est vide');
      return;
    }

    this.payState.set('processing');
    this.error.set(null);

    this.orderService.createOrder(this.slug, this.tableId, this.cart.cartEntries()).subscribe({
      next: order => {
        this.createdOrderId.set(order.id);
        this.openCheckout(order.id);
      },
      error: err => this.fail(this.extractErrorMessage(err, 'Erreur lors de la création de la commande')),
    });
  }

  retryPayment(): void {
    if (!this.createdOrderId()) {
      this.error.set(null);
      this.payState.set('idle');
      return;
    }
    this.error.set(null);
    this.payState.set('processing');
    this.openCheckout(this.createdOrderId()!);
  }

  goBackToMenu(): void {
    void this.router.navigate(['/menu', this.slug, this.tableId]);
  }

  private openCheckout(orderId: string): void {
    this.orderService.createCheckoutSession(orderId).subscribe({
      next: session => {
        this.cart.clear();
        window.location.href = session.checkoutUrl;
      },
      error: err => this.fail(this.extractErrorMessage(err, 'Erreur lors de la création du paiement')),
    });
  }

  private fail(message: string): void {
    this.error.set(message);
    this.payState.set('idle');
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const apiError = error as { error?: { message?: string; error?: string } };
    return apiError.error?.message ?? apiError.error?.error ?? fallback;
  }
}
