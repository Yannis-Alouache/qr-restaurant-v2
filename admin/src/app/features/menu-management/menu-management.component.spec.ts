import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { ImageService } from '../../core/services/image.service';
import { MenuService } from '../../core/services/menu.service';
import { MenuManagementComponent } from './menu-management.component';

describe('MenuManagementComponent', () => {
  it('prevents combo variant creation when the selected category does not allow menus', () => {
    const createMenuItem = vi.fn();
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => undefined);

    TestBed.configureTestingModule({
      imports: [MenuManagementComponent],
      providers: [
        {
          provide: MenuService,
          useValue: {
            categories: signal([
              { id: 'category-1', name: 'Desserts', imagePath: null, position: 0, hasMenu: false },
            ]),
            menuItems: signal([]),
            compositions: signal([]),
            loadCategories: vi.fn().mockReturnValue(of([])),
            loadMenuItems: vi.fn().mockReturnValue(of([])),
            loadCompositions: vi.fn().mockReturnValue(of([])),
            createCategory: vi.fn(),
            updateCategory: vi.fn(),
            deleteCategory: vi.fn(),
            createMenuItem,
            updateMenuItem: vi.fn(),
            toggleAvailability: vi.fn(),
            deleteMenuItem: vi.fn(),
            createComposition: vi.fn(),
            deleteComposition: vi.fn(),
          },
        },
        {
          provide: ImageService,
          useValue: {
            upload: vi.fn(),
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(MenuManagementComponent);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.itemForm.setValue({
      categoryId: 'category-1',
      name: 'Brownie',
      description: '',
      price: 5,
      isCombo: true,
      comboPrice: 8,
    });

    component.saveItem();

    expect(createMenuItem).not.toHaveBeenCalled();
    expect(alertSpy).toHaveBeenCalledWith(
      'La catégorie sélectionnée ne permet pas de créer une variante menu',
    );
  });

  it('surfaces combo variant creation errors instead of swallowing them', () => {
    const loadMenuItems = vi.fn().mockReturnValue(of([]));
    const createMenuItem = vi
      .fn()
      .mockReturnValueOnce(
        of({
          id: 'item-1',
          categoryId: 'category-1',
          name: 'Burger',
          description: null,
          price: 12,
          imagePath: null,
          available: true,
          menuVariantOf: null,
        }),
      )
      .mockReturnValueOnce(
        throwError(() => ({
          error: { error: 'La catégorie sélectionnée ne permet pas de créer une variante menu' },
        })),
      );
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => undefined);

    TestBed.configureTestingModule({
      imports: [MenuManagementComponent],
      providers: [
        {
          provide: MenuService,
          useValue: {
            categories: signal([
              { id: 'category-1', name: 'Burgers', imagePath: null, position: 0, hasMenu: true },
            ]),
            menuItems: signal([]),
            compositions: signal([]),
            loadCategories: vi.fn().mockReturnValue(of([])),
            loadMenuItems,
            loadCompositions: vi.fn().mockReturnValue(of([])),
            createCategory: vi.fn(),
            updateCategory: vi.fn(),
            deleteCategory: vi.fn(),
            createMenuItem,
            updateMenuItem: vi.fn(),
            toggleAvailability: vi.fn(),
            deleteMenuItem: vi.fn(),
            createComposition: vi.fn(),
            deleteComposition: vi.fn(),
          },
        },
        {
          provide: ImageService,
          useValue: {
            upload: vi.fn(),
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(MenuManagementComponent);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.itemForm.setValue({
      categoryId: 'category-1',
      name: 'Burger',
      description: '',
      price: 12,
      isCombo: true,
      comboPrice: 15,
    });

    component.saveItem();

    expect(createMenuItem).toHaveBeenCalledTimes(2);
    expect(loadMenuItems).toHaveBeenCalled();
    expect(alertSpy).toHaveBeenCalledWith(
      'La catégorie sélectionnée ne permet pas de créer une variante menu',
    );
  });

  it('keeps an empty description in the update payload so the backend can clear it', () => {
    const updateMenuItem = vi.fn().mockReturnValue(of({}));

    TestBed.configureTestingModule({
      imports: [MenuManagementComponent],
      providers: [
        {
          provide: MenuService,
          useValue: {
            categories: signal([
              { id: 'category-1', name: 'Burgers', imagePath: null, position: 0, hasMenu: true },
            ]),
            menuItems: signal([]),
            compositions: signal([]),
            loadCategories: vi.fn().mockReturnValue(of([])),
            loadMenuItems: vi.fn().mockReturnValue(of([])),
            loadCompositions: vi.fn().mockReturnValue(of([])),
            createCategory: vi.fn(),
            updateCategory: vi.fn(),
            deleteCategory: vi.fn(),
            createMenuItem: vi.fn(),
            updateMenuItem,
            toggleAvailability: vi.fn(),
            deleteMenuItem: vi.fn(),
            createComposition: vi.fn(),
            deleteComposition: vi.fn(),
          },
        },
        {
          provide: ImageService,
          useValue: {
            upload: vi.fn(),
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(MenuManagementComponent);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.editingItemId.set('item-1');
    component.itemForm.setValue({
      categoryId: 'category-1',
      name: 'Burger',
      description: '',
      price: 12,
      isCombo: false,
      comboPrice: 0,
    });

    component.saveItem();

    expect(updateMenuItem).toHaveBeenCalledWith('item-1', {
      categoryId: 'category-1',
      name: 'Burger',
      description: '',
      price: 12,
    });
  });

  it('only proposes standalone available items as composition candidates', () => {
    TestBed.configureTestingModule({
      imports: [MenuManagementComponent],
      providers: [
        {
          provide: MenuService,
          useValue: {
            categories: signal([]),
            menuItems: signal([
              {
                id: 'item-1',
                categoryId: 'category-1',
                name: 'Frites',
                description: null,
                price: 3.5,
                imagePath: null,
                available: true,
                menuVariantOf: null,
              },
              {
                id: 'item-2',
                categoryId: 'category-1',
                name: 'Menu Burger',
                description: null,
                price: 10,
                imagePath: null,
                available: true,
                menuVariantOf: 'base-1',
              },
              {
                id: 'item-3',
                categoryId: 'category-1',
                name: 'Coca',
                description: null,
                price: 2.5,
                imagePath: null,
                available: false,
                menuVariantOf: null,
              },
            ]),
            compositions: signal([]),
            loadCategories: vi.fn().mockReturnValue(of([])),
            loadMenuItems: vi.fn().mockReturnValue(of([])),
            loadCompositions: vi.fn().mockReturnValue(of([])),
            createCategory: vi.fn(),
            updateCategory: vi.fn(),
            deleteCategory: vi.fn(),
            createMenuItem: vi.fn(),
            updateMenuItem: vi.fn(),
            toggleAvailability: vi.fn(),
            deleteMenuItem: vi.fn(),
            createComposition: vi.fn(),
            deleteComposition: vi.fn(),
          },
        },
        {
          provide: ImageService,
          useValue: {
            upload: vi.fn(),
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(MenuManagementComponent);
    const component = fixture.componentInstance;

    fixture.detectChanges();

    expect(component.allItemsForCompositions()).toEqual([
      {
        id: 'item-1',
        categoryId: 'category-1',
        name: 'Frites',
        description: null,
        price: 3.5,
        imagePath: null,
        available: true,
        menuVariantOf: null,
      },
    ]);
  });
});
