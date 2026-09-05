import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { tableIdGuard } from './table-id.guard';

@Component({ selector: 'app-dummy', template: '' })
class DummyComponent {}

describe('tableIdGuard', () => {
  const validTableId = '58338618-3433-4a4a-808e-cfea0b1f9c02';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'menu/:slug/:tableId', component: DummyComponent, canActivate: [tableIdGuard] },
          { path: 'checkout/:slug/:tableId', component: DummyComponent, canActivate: [tableIdGuard] },
          { path: 'qr-invalide/:slug', component: DummyComponent },
        ]),
      ],
    });
  });

  it('laisse passer un UUID de table valide', async () => {
    const router = TestBed.inject(Router);
    await router.navigateByUrl(`/menu/naia/${validTableId}`);
    expect(router.url).toBe(`/menu/naia/${validTableId}`);
  });

  it('accepte un UUID en majuscules', async () => {
    const router = TestBed.inject(Router);
    await router.navigateByUrl(`/checkout/naia/${validTableId.toUpperCase()}`);
    expect(router.url).toBe(`/checkout/naia/${validTableId.toUpperCase()}`);
  });

  it('bloque un numéro de table en conservant le slug dans la redirection', async () => {
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/menu/naia/1');
    expect(router.url).toBe('/qr-invalide/naia');
  });

  it('bloque aussi les routes profondes comme le checkout', async () => {
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/checkout/naia-burger/table-2');
    expect(router.url).toBe('/qr-invalide/naia-burger');
  });

  it('bloque un UUID tronqué', async () => {
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/menu/naia/58338618-3433-4a4a-808e-cfea0b1f9c0');
    expect(router.url).toBe('/qr-invalide/naia');
  });
});
