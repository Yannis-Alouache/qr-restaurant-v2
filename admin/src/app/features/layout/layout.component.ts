import { Component, HostListener, OnInit, inject, signal, computed } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { RestaurantService } from '../../core/services/restaurant.service';
import { OrderService } from '../../core/services/order.service';
import { ToastService } from '../../core/services/toast.service';
import { ConfirmService } from '../../core/services/confirm.service';
import { ADMIN_ICONS } from '../../core/icons';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ...ADMIN_ICONS],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss',
})
export class LayoutComponent implements OnInit {
  private auth = inject(AuthService);
  private restaurantService = inject(RestaurantService);
  private orderService = inject(OrderService);
  private router = inject(Router);
  readonly toast = inject(ToastService);
  readonly confirm = inject(ConfirmService);

  restaurant = this.restaurantService.restaurant;
  email = this.auth.email;

  profileOpen = signal(false);
  notificationsOpen = signal(false);

  /** Live count of in-progress orders — drives the bell badge. */
  activeOrderCount = computed(() =>
    this.orderService.orders().filter(o => o.status !== 'servie').length,
  );

  initials = computed(() => {
    const name = this.restaurant()?.name ?? this.email() ?? '?';
    const parts = name.trim().split(/\s+/);
    return (parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '');
  });

  ngOnInit(): void {
    this.restaurantService.loadRestaurant().subscribe({
      error: (err) => {
        if (err.status === 401 || err.status === 403) {
          this.auth.logout().subscribe(() => this.router.navigate(['/login']));
        } else {
          this.router.navigate(['/onboarding']);
        }
      },
    });
  }

  toggleNotifications(): void {
    this.notificationsOpen.update(v => !v);
    this.closeProfile();
  }

  closeNotifications(): void {
    this.notificationsOpen.set(false);
  }

  toggleProfile(): void {
    this.profileOpen.update(v => !v);
    this.closeNotifications();
  }

  closeProfile(): void {
    this.profileOpen.set(false);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closeProfile();
    this.closeNotifications();
    this.confirm.cancel();
  }

  logout(): void {
    this.closeProfile();
    this.auth.logout().subscribe(() => this.router.navigate(['/login']));
  }
}
