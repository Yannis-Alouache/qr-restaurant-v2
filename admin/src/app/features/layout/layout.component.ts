import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { RestaurantService } from '../../core/services/restaurant.service';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss'
})
export class LayoutComponent implements OnInit {
  private auth = inject(AuthService);
  private restaurant = inject(RestaurantService);
  private router = inject(Router);

  restaurantName = '';

  ngOnInit(): void {
    this.restaurant.loadRestaurant().subscribe({
      next: (r) => this.restaurantName = r.name,
      error: () => this.router.navigate(['/onboarding'])
    });
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
