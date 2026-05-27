import { MenuView } from '../models/menu.model';

const ABSOLUTE_URL_PATTERN = /^[a-z][a-z\d+\-.]*:/i;

export function normalizeMenuAssetUrl(
  assetUrl: string | null | undefined,
  baseUrl: string,
): string | null {
  const trimmedAssetUrl = assetUrl?.trim();

  if (!trimmedAssetUrl) {
    return null;
  }

  if (ABSOLUTE_URL_PATTERN.test(trimmedAssetUrl)) {
    return trimmedAssetUrl;
  }

  return new URL(trimmedAssetUrl, baseUrl).toString();
}

export function normalizeMenuAssetUrls(menu: MenuView, baseUrl: string): MenuView {
  return {
    ...menu,
    restaurant: {
      ...menu.restaurant,
      logoPath: normalizeMenuAssetUrl(menu.restaurant.logoPath, baseUrl),
    },
    categories: menu.categories.map((category) => ({
      ...category,
      imagePath: normalizeMenuAssetUrl(category.imagePath, baseUrl),
      items: category.items.map((item) => ({
        ...item,
        imagePath: normalizeMenuAssetUrl(item.imagePath, baseUrl),
      })),
    })),
    compositions: menu.compositions.map((composition) => ({
      ...composition,
      menuItemImagePath: normalizeMenuAssetUrl(composition.menuItemImagePath, baseUrl),
    })),
  };
}
