import { MenuView } from '../models/menu.model';
import { normalizeMenuAssetUrl, normalizeMenuAssetUrls } from './menu-asset-url.util';

describe('menu asset url util', () => {
  const baseUrl = 'https://menu.example.com/';

  it('normalizes root-relative and bare relative asset URLs against the application origin', () => {
    expect(normalizeMenuAssetUrl('/uploads/burger.jpg', baseUrl)).toBe(
      'https://menu.example.com/uploads/burger.jpg',
    );
    expect(normalizeMenuAssetUrl('uploads/category.jpg', baseUrl)).toBe(
      'https://menu.example.com/uploads/category.jpg',
    );
    expect(normalizeMenuAssetUrl('./logos/brand.png', baseUrl)).toBe(
      'https://menu.example.com/logos/brand.png',
    );
  });

  it('preserves already absolute asset URLs and clears blank ones', () => {
    expect(normalizeMenuAssetUrl('https://cdn.example.com/assets/item.webp', baseUrl)).toBe(
      'https://cdn.example.com/assets/item.webp',
    );
    expect(normalizeMenuAssetUrl('data:image/png;base64,AAAA', baseUrl)).toBe(
      'data:image/png;base64,AAAA',
    );
    expect(normalizeMenuAssetUrl('   ', baseUrl)).toBeNull();
    expect(normalizeMenuAssetUrl(null, baseUrl)).toBeNull();
  });

  it('normalizes every menu asset field used by the client experience', () => {
    const menu: MenuView = {
      restaurant: {
        id: 'resto-1',
        name: 'Naia Burger',
        slug: 'naia-burger',
        address: 'Paris',
        logoPath: 'logos/naia.png',
        themeId: 'chaud',
      },
      categories: [
        {
          id: 'cat-1',
          name: 'Burgers',
          imagePath: '/media/categories/burgers.jpg',
          position: 0,
          hasMenu: true,
          items: [
            {
              id: 'item-1',
              name: 'Burger signature',
              description: 'Steak et cheddar',
              price: 13.9,
              imagePath: 'media/items/burger-signature.jpg',
              available: true,
              menuVariantOf: null,
            },
          ],
        },
      ],
      compositions: [
        {
          id: 'comp-1',
          compositionType: 'boisson',
          menuItemId: 'drink-1',
          menuItemName: 'Cola',
          menuItemImagePath: 'https://cdn.example.com/drinks/cola.jpg',
          supplementPrice: 0,
        },
      ],
    };

    expect(normalizeMenuAssetUrls(menu, baseUrl)).toEqual({
      ...menu,
      restaurant: {
        ...menu.restaurant,
        logoPath: 'https://menu.example.com/logos/naia.png',
      },
      categories: [
        {
          ...menu.categories[0],
          imagePath: 'https://menu.example.com/media/categories/burgers.jpg',
          items: [
            {
              ...menu.categories[0].items[0],
              imagePath: 'https://menu.example.com/media/items/burger-signature.jpg',
            },
          ],
        },
      ],
      compositions: [
        {
          ...menu.compositions[0],
          menuItemImagePath: 'https://cdn.example.com/drinks/cola.jpg',
        },
      ],
    });
  });
});
