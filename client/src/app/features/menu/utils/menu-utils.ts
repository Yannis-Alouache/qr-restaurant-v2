import { CategoryView, CompositionEntry, ItemView, MenuView } from '../models/menu.model';

/** Palette de couleurs de repli pour les catégories sans image (tokens maquette). */
const CATEGORY_PALETTE = [
  'var(--cat-burgers)',
  'var(--cat-sandwichs)',
  'var(--cat-salades)',
  'var(--cat-pizzas)',
  'var(--cat-desserts)',
  'var(--cat-boissons)',
  'var(--cat-accompagnements)',
];

export function categoryFallbackColor(index: number): string {
  return CATEGORY_PALETTE[index % CATEGORY_PALETTE.length];
}

/** Image de couverture d'une catégorie : la sienne, sinon son premier item illustré. */
export function categoryCover(category: CategoryView): string | null {
  if (category.imagePath) return category.imagePath;
  const illustrated = category.items.find(item => item.imagePath);
  return illustrated?.imagePath ?? null;
}

/** Bannière du restaurant : celle configurée, sinon la première image de catégorie. */
export function restaurantCover(menu: MenuView): string | null {
  if (menu.restaurant.coverPath) return menu.restaurant.coverPath;
  for (const category of menu.categories) {
    const cover = categoryCover(category);
    if (cover) return cover;
  }
  return null;
}

/** Items commandables seuls (ni variante menu, ni indisponible). */
export function standaloneItems(category: CategoryView): ItemView[] {
  return category.items.filter(item => item.menuVariantOf === null && item.available);
}

/** Variante menu disponible correspondant à un plat de base. */
export function menuVariantOf(category: CategoryView, item: ItemView): ItemView | null {
  return (
    category.items.find(
      candidate => candidate.menuVariantOf === item.id && candidate.available,
    ) ?? null
  );
}

/** Plats de base composables en menu (ils possèdent une variante disponible). */
export function menuEligibleItems(category: CategoryView): ItemView[] {
  return standaloneItems(category).filter(item => menuVariantOf(category, item) !== null);
}

/** La catégorie propose au moins une formule menu réelle. */
export function categoryHasMenus(category: CategoryView): boolean {
  return menuEligibleItems(category).length > 0;
}

/** Accompagnements / boissons éligibles extraits des compositions. */
export function compositionsOf(
  compositions: CompositionEntry[],
  type: 'accompagnement' | 'boisson',
): CompositionEntry[] {
  return compositions.filter(c => c.compositionType === type);
}

/**
 * Économie minimale garantie par la formule menu pour une catégorie.
 *
 * menu = variante + suppléments ; achat séparé = plat + accompagnement + boisson
 * au prix unitaire. L'économie vaut :
 *   (base − variante) + Δaccompagnement + Δboisson
 * avec Δ = prix unitaire − supplément (0 si l'item n'est pas vendu seul).
 * Retourne null si aucune économie n'est démontrable.
 */
export function minMenuSavings(
  category: CategoryView,
  compositions: CompositionEntry[],
  allCategories: CategoryView[] = [category],
): number | null {
  const sides = compositionsOf(compositions, 'accompagnement');
  const drinks = compositionsOf(compositions, 'boisson');
  if (sides.length === 0 || drinks.length === 0) return null;

  const standalonePrices = new Map<string, number>();
  for (const cat of allCategories) {
    for (const item of cat.items) {
      if (item.menuVariantOf === null && item.available) {
        standalonePrices.set(item.id, item.price);
      }
    }
  }

  const delta = (entry: CompositionEntry): number =>
    (standalonePrices.get(entry.menuItemId) ?? entry.supplementPrice) - entry.supplementPrice;

  const minSideDelta = Math.min(...sides.map(delta));
  const minDrinkDelta = Math.min(...drinks.map(delta));

  let best: number | null = null;
  for (const item of menuEligibleItems(category)) {
    const variant = menuVariantOf(category, item)!;
    const savings = item.price - variant.price + minSideDelta + minDrinkDelta;
    if (savings > 0 && (best === null || savings < best)) {
      best = savings;
    }
  }
  return best;
}
