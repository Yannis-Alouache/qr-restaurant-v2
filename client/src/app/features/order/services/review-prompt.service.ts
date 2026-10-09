import { Injectable, signal } from '@angular/core';

interface ReviewContext {
  slug: string;
  tableId: string;
  orderId: string;
  status: string;
}

const STORAGE_KEY = 'menzo:review-prompt';

/**
 * Mémorise (le temps de la session) la dernière commande suivie sur cet
 * appareil, pour proposer l'avis Google au bon moment : uniquement une fois
 * la commande servie, et seulement sur le menu de la table d'origine.
 * Le slug est retenu au moment du paiement (la page de suivi, ouverte en
 * retour de Stripe, n'a pas le slug dans son URL).
 */
@Injectable({ providedIn: 'root' })
export class ReviewPromptService {
  private readonly context = signal<ReviewContext | null>(this.readStorage());

  readonly reviewContext = this.context.asReadonly();

  track(slug: string, tableId: string, orderId: string): void {
    this.setContext({ slug, tableId, orderId, status: 'en_attente_paiement' });
  }

  setStatus(orderId: string, status: string): void {
    const current = this.context();
    if (!current || current.orderId !== orderId) {
      return;
    }
    this.setContext({ ...current, status });
  }

  /** Lien visible seulement après le service, sur la table d'origine. */
  servedOnThisTable(slug: string, tableId: string): boolean {
    const current = this.context();
    return !!current
      && current.status === 'servie'
      && current.slug === slug
      && current.tableId === tableId;
  }

  private setContext(context: ReviewContext): void {
    this.context.set(context);
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(context));
    } catch {
      // sessionStorage indisponible (navigation privée stricte) : l'invitation
      // vivra seulement dans le signal, le temps de la page.
    }
  }

  private readStorage(): ReviewContext | null {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as ReviewContext) : null;
    } catch {
      return null;
    }
  }
}
