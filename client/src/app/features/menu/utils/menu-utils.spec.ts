import {
  categoryHasMenus,
  menuEligibleItems,
  menuVariantOf,
  minMenuSavings,
  standaloneItems,
} from './menu-utils';
import { CategoryView, CompositionEntry, ItemView } from '../models/menu.model';

function item(partial: Partial<ItemView> & { id: string }): ItemView {
  return {
    name: partial.id,
    description: null,
    price: 6.9,
    imagePath: null,
    available: true,
    menuVariantOf: null,
    ...partial,
  };
}

function composition(partial: Partial<CompositionEntry> & { id: string }): CompositionEntry {
  return {
    compositionType: 'accompagnement',
    menuItemId: partial.id,
    menuItemName: partial.id,
    menuItemImagePath: null,
    supplementPrice: 0,
    ...partial,
  };
}

const category: CategoryView = {
  id: 'cat-1',
  name: 'Burgers',
  imagePath: null,
  position: 0,
  hasMenu: true,
  items: [
    item({ id: 'base-1', name: 'Burger classique', price: 6.9 }),
    item({ id: 'variant-1', name: 'Menu Burger classique', price: 10.4, menuVariantOf: 'base-1' }),
    item({ id: 'variant-unavailable', name: 'Menu indisponible', price: 9, menuVariantOf: 'base-1', available: false }),
    item({ id: 'base-2', name: 'Burger bacon', price: 8.5 }),
    item({ id: 'solo-unavailable', name: 'Périmé', price: 3, available: false }),
  ],
};

const compositions: CompositionEntry[] = [
  composition({ id: 'frites', menuItemName: 'Frites', supplementPrice: 0 }),
  composition({ id: 'nuggets', menuItemName: 'Nuggets x6', supplementPrice: 1.5 }),
  composition({ id: 'coca', menuItemName: 'Coca-Cola', compositionType: 'boisson', supplementPrice: 0 }),
  composition({ id: 'fanta', menuItemName: 'Fanta', compositionType: 'boisson', supplementPrice: 0 }),
];

describe('menu-utils', () => {
  it('lists only standalone available items for the solo grid', () => {
    expect(standaloneItems(category).map(i => i.id)).toEqual(['base-1', 'base-2']);
  });

  it('resolves the available menu variant of a base item', () => {
    expect(menuVariantOf(category, category.items[0])!.id).toBe('variant-1');
    expect(menuVariantOf(category, category.items[3])).toBeNull();
  });

  it('restricts the menu stepper to base items with an available variant', () => {
    expect(menuEligibleItems(category).map(i => i.id)).toEqual(['base-1']);
  });

  it('detects whether a category actually offers menus', () => {
    expect(categoryHasMenus(category)).toBe(true);
    expect(
      categoryHasMenus({ ...category, items: category.items.filter(i => i.menuVariantOf === null) }),
    ).toBe(false);
  });

  it('computes the guaranteed menu savings from the cheapest composition', () => {
    // Base 6.9 − variante 10.4, suppléments inconnus en vente seule → Δ = 0 → pas d'économie
    expect(minMenuSavings(category, compositions)).toBeNull();
  });

  it('computes savings when buying separately is more expensive', () => {
    const cheapVariant = category.items.map(i =>
      i.id === 'variant-1' ? { ...i, price: 6.4 } : i,
    );
    // 6.9 − 6.4 = 0.5 € d'économie garantie même sans vente seule des compositions
    expect(minMenuSavings({ ...category, items: cheapVariant }, compositions)).toBe(0.5);
  });

  it('uses standalone composition prices when they are sold individually', () => {
    // Frites vendues seules à 3.50 (supplément 0) → Δ = 3.50 ; Coca 2.90 (supplément 0) → Δ = 2.90
    const sideCategory: CategoryView = {
      id: 'cat-sides',
      name: 'Accompagnements',
      imagePath: null,
      position: 1,
      hasMenu: false,
      items: [
        item({ id: 'frites', name: 'Frites', price: 3.5 }),
        item({ id: 'nuggets', name: 'Nuggets x6', price: 5 }),
      ],
    };
    const drinkCategory: CategoryView = {
      id: 'cat-drinks',
      name: 'Boissons',
      imagePath: null,
      position: 2,
      hasMenu: false,
      items: [
        item({ id: 'coca', name: 'Coca-Cola', price: 2.9 }),
        item({ id: 'fanta', name: 'Fanta', price: 2.9 }),
      ],
    };
    // Δ frites 3.5−0, Δ nuggets 5−1.5 ; Δ coca/fanta 2.9−0
    // 6.9 − 10.4 + 3.5 + 2.9 = 2.9 € d'économie garantie
    expect(minMenuSavings(category, compositions, [category, sideCategory, drinkCategory])).toBe(2.9);
  });
});
