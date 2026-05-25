export interface MenuView {
  restaurant: RestaurantInfo;
  categories: CategoryView[];
  compositions: CompositionEntry[];
}

export interface RestaurantInfo {
  id: string;
  name: string;
  slug: string;
  address: string | null;
  logoPath: string | null;
  themeId: string;
}

export interface CategoryView {
  id: string;
  name: string;
  imagePath: string | null;
  position: number;
  hasMenu: boolean;
  items: ItemView[];
}

export interface ItemView {
  id: string;
  name: string;
  description: string | null;
  price: number;
  imagePath: string | null;
  available: boolean;
  menuVariantOf: string | null;
}

export interface CompositionEntry {
  id: string;
  compositionType: 'accompagnement' | 'boisson';
  menuItemId: string;
  menuItemName: string;
  menuItemImagePath: string | null;
  supplementPrice: number;
}

export interface OrderResponse {
  id: string;
  status: string;
  total: number;
  createdAt: string;
}

export interface OrderDetailResponse {
  id: string;
  status: string;
  total: number;
  createdAt: string;
  items: OrderItemResponse[];
}

export interface OrderItemResponse {
  id: string;
  menuItemId: string;
  name: string;
  quantity: number;
  unitPrice: number;
  menuGroupId: string | null;
  menuRole: string | null;
}

export interface CheckoutSessionResponse {
  checkoutUrl: string;
}
