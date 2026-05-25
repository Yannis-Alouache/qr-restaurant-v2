export interface CartStandaloneEntry {
  type: 'standalone';
  cartId: string;
  itemId: string;
  name: string;
  price: number;
  imagePath: string | null;
  quantity: number;
}

export interface CartComboEntry {
  type: 'combo';
  cartId: string;
  mainItem: { id: string; name: string; price: number; imagePath: string | null };
  sideItem: { id: string; name: string; supplementPrice: number; imagePath: string | null };
  drinkItem: { id: string; name: string; supplementPrice: number; imagePath: string | null };
  quantity: number;
}

export type CartEntry = CartStandaloneEntry | CartComboEntry;
