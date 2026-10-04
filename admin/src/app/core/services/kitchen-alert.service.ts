import { Injectable, signal } from '@angular/core';

/** Rappel sonore tant qu'une commande « nouvelle » n'a pas été prise en charge. */
const REPEAT_DELAY_MS = 30_000;
const STORAGE_KEY = 'kitchen.alerts.enabled';

export interface NewOrderInfo {
  tableNumber: number;
  total: number;
}

/** Minimaliste : seule la surface réellement utilisée est typée (l'API complète dépend de la version de lib.dom). */
interface WakeLockSentinelLike {
  release(): Promise<void>;
  addEventListener(type: 'release', listener: () => void): void;
}
type NavigatorWithWakeLock = Navigator & {
  wakeLock?: { request(type: 'screen'): Promise<WakeLockSentinelLike> };
};

/**
 * Alertes « poste cuisine » pour la page Commandes : son immédiat à l'arrivée
 * d'une commande, répété tant qu'elle n'est pas prise en charge, notification
 * système quand l'onglet est en arrière-plan et maintien de l'écran allumé.
 *
 * Le son est synthétisé via la Web Audio API (pas d'asset à charger) et ne
 * peut être déverrouillé que par un geste utilisateur — c'est le rôle du
 * bouton « Son » de la page, qui appelle `enable()`.
 */
@Injectable({ providedIn: 'root' })
export class KitchenAlertService {
  private readonly _enabled = signal(this.readStoredPreference());
  private audioCtx: AudioContext | null = null;
  private repeatTimer: ReturnType<typeof setInterval> | null = null;
  private wakeLock: WakeLockSentinelLike | null = null;

  readonly enabled = this._enabled.asReadonly();

  constructor() {
    if (this._enabled()) {
      // Préférence déjà active après rechargement : le son attendra le
      // prochain geste utilisateur, l'écran peut être verrouillé tout de
      // suite. Différé hors du constructeur, qui reste sans effet de bord.
      queueMicrotask(() => {
        this.armLazyAudioUnlock();
        void this.refreshWakeLock();
      });
    }
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible' && this._enabled()) {
        void this.refreshWakeLock();
      }
    });
  }

  /**
   * Active les alertes. À appeler depuis un clic : le geste déverrouille
   * l'AudioContext et autorise la demande de permission Notification.
   * Renvoie true si les notifications système sont utilisables — le son
   * fonctionne dans tous les cas.
   */
  async enable(): Promise<boolean> {
    this.unlockAudio();
    const permission = await this.requestNotificationPermission();
    this._enabled.set(true);
    this.persist(true);
    void this.refreshWakeLock();
    return permission === 'granted';
  }

  disable(): void {
    this.stopRepeat();
    this.releaseWakeLock();
    this._enabled.set(false);
    this.persist(false);
  }

  /** Arrivée d'une commande : carillon immédiat, notification, boucle de rappel. */
  onNewOrder(info?: NewOrderInfo): void {
    if (!this._enabled()) return;
    this.chime();
    this.notify(info);
    this.startRepeat();
  }

  /** Le board notifie le nombre de commandes « nouvelle » restantes : zéro = on cesse de rappeler. */
  setPendingNewOrders(count: number): void {
    if (count <= 0) {
      this.stopRepeat();
    }
  }

  private chime(): void {
    const ctx = this.audioCtx ?? this.createAudioContext();
    if (!ctx) return;
    if (ctx.state === 'suspended') {
      void ctx.resume().catch(() => {});
    }
    this.audioCtx = ctx;
    // Double note montante (la4 → ré5), façon cloche de cuisine.
    this.playTone(ctx, 880, ctx.currentTime);
    this.playTone(ctx, 1174.66, ctx.currentTime + 0.18);
  }

  private playTone(ctx: AudioContext, frequency: number, start: number): void {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.value = frequency;
    gain.gain.setValueAtTime(0.0001, start);
    gain.gain.exponentialRampToValueAtTime(0.5, start + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.0001, start + 0.9);
    osc.connect(gain).connect(ctx.destination);
    osc.start(start);
    osc.stop(start + 1);
  }

  private notify(info?: NewOrderInfo): void {
    if (!this.canNotify()) return;
    const body = info
      ? `Table ${info.tableNumber} — ${info.total.toFixed(2).replace('.', ',')} €`
      : 'Une nouvelle commande vient d\'arriver';
    this.showSystemNotification('Nouvelle commande', body);
  }

  private canNotify(): boolean {
    // Inutile quand la page est visible : le board se met à jour sous les yeux.
    return typeof Notification !== 'undefined'
      && Notification.permission === 'granted'
      && this.isPageHidden();
  }

  private startRepeat(): void {
    if (this.repeatTimer) return;
    this.repeatTimer = setInterval(() => this.chime(), REPEAT_DELAY_MS);
  }

  private stopRepeat(): void {
    if (this.repeatTimer) {
      clearInterval(this.repeatTimer);
      this.repeatTimer = null;
    }
  }

  private unlockAudio(): void {
    const ctx = this.audioCtx ?? this.createAudioContext();
    this.audioCtx = ctx;
    if (ctx?.state === 'suspended') {
      void ctx.resume().catch(() => {});
    }
  }

  private armLazyAudioUnlock(): void {
    // Après rechargement, l'AudioContext reste verrouillé jusqu'au prochain
    // geste : on retente au premier clic, quel que soit l'élément visé.
    document.addEventListener('pointerdown', () => {
      if (this._enabled()) this.unlockAudio();
    }, { once: true, capture: true });
  }

  private async refreshWakeLock(): Promise<void> {
    try {
      const sentinel = await this.acquireScreenLock();
      if (sentinel) {
        // Le navigateur peut révoquer le verrou (extinction, économie d'énergie).
        sentinel.addEventListener('release', () => {
          if (this.wakeLock === sentinel) this.wakeLock = null;
        });
        this.wakeLock = sentinel;
      }
    } catch {
      // Refusé (préférence navigateur, mode économie) : le son reste la garantie.
    }
  }

  private releaseWakeLock(): void {
    void this.wakeLock?.release().catch(() => {});
    this.wakeLock = null;
  }

  private readStoredPreference(): boolean {
    try {
      return localStorage.getItem(STORAGE_KEY) === 'true';
    } catch {
      return false;
    }
  }

  private persist(enabled: boolean): void {
    try {
      localStorage.setItem(STORAGE_KEY, String(enabled));
    } catch {
      // Stockage indisponible : préférence valable pour la session uniquement.
    }
  }

  // ─── Sutures pour les tests (même approche que WebSocketService) ──────
  protected createAudioContext(): AudioContext | null {
    return typeof AudioContext === 'undefined' ? null : new AudioContext();
  }
  protected requestNotificationPermission(): Promise<NotificationPermission> {
    return typeof Notification === 'undefined'
      ? Promise.resolve('denied')
      : Notification.requestPermission();
  }
  protected showSystemNotification(title: string, body: string): void {
    const notification = new Notification(title, { body });
    notification.onclick = () => {
      window.focus();
      notification.close();
    };
  }
  protected async acquireScreenLock(): Promise<WakeLockSentinelLike | null> {
    const wakeLock = (navigator as NavigatorWithWakeLock).wakeLock;
    return wakeLock ? wakeLock.request('screen') : null;
  }
  protected isPageHidden(): boolean {
    return document.hidden;
  }
}
