import { TestBed } from '@angular/core/testing';
import { NotFoundScreenComponent } from './not-found-screen.component';

describe('NotFoundScreenComponent', () => {
  it('affiche un écran bloquant sans aucune action proposée', () => {
    TestBed.configureTestingModule({ imports: [NotFoundScreenComponent] });
    const fixture = TestBed.createComponent(NotFoundScreenComponent);
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Page introuvable');
    expect(element.textContent).toContain('QR code présent sur votre table');
    expect(element.querySelectorAll('button, a, [role="button"]').length).toBe(0);
  });
});
