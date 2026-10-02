import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MenuService } from '../../services/menu.service';
import { InvalidQrScreenComponent } from './invalid-qr-screen.component';

describe('InvalidQrScreenComponent', () => {
  function createFixture(menuService: Record<string, unknown>, slug: string) {
    TestBed.configureTestingModule({
      imports: [InvalidQrScreenComponent],
      providers: [
        { provide: MenuService, useValue: menuService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ slug }) } },
        },
      ],
    });
    const fixture = TestBed.createComponent(InvalidQrScreenComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    document.documentElement.setAttribute('data-theme', 'classique');
  });

  afterEach(() => {
    document.documentElement.removeAttribute('data-theme');
  });

  it('affiche le message bloquant et ne propose aucune action', () => {
    const fixture = createFixture({ getMenu: vi.fn().mockReturnValue(of(makeMenu('chaud'))) }, 'naia');

    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('QR code non reconnu');
    expect(element.textContent).toContain('scanner le QR code présent sur votre table');
    expect(element.querySelectorAll('button, a, [role="button"]').length).toBe(0);
  });

  it('applique le thème choisi par le admin du restaurant', () => {
    createFixture({ getMenu: vi.fn().mockReturnValue(of(makeMenu('elegant'))) }, 'naia-burger');

    expect(document.documentElement.getAttribute('data-theme')).toBe('elegant');
  });

  it('conserve le thème courant quand le slug est inconnu', () => {
    createFixture({ getMenu: vi.fn().mockReturnValue(throwError(() => new Error('404'))) }, 'inconnu');

    expect(document.documentElement.getAttribute('data-theme')).toBe('classique');
  });
});

function makeMenu(themeId: string) {
  return {
    restaurant: {
      id: 'r1',
      name: 'Naia Burger',
      slug: 'naia-burger',
      address: null,
      logoPath: null,
      coverPath: null,
      themeId,
    },
    categories: [],
    compositions: [],
  };
}
