import { Client, IMessage, StompConfig, StompSubscription } from '@stomp/stompjs';
import { Injectable } from '@angular/core';
import SockJS from 'sockjs-client';

@Injectable({ providedIn: 'root' })
export class OrderStatusRealtimeService {
  private client: Client | null = null;
  private subscription: StompSubscription | null = null;

  /**
   * Suit le statut d'une commande via WebSocket.
   * `onConnected` est appelé à chaque (re)connexion : le consommateur peut
   * alors rafraîchir son état pour ne manquer aucune transition émise
   * pendant que la connexion était en cours d'établissement.
   */
  connect(
    orderId: string,
    onStatusChange: (status: string) => void,
    onConnected?: () => void,
  ): void {
    if (!orderId || this.client?.active) {
      return;
    }

    const client = this.createClient({
      webSocketFactory: () => this.createSocket(),
      reconnectDelay: 5000,
      onConnect: () => {
        this.subscription?.unsubscribe();
        this.subscription = client.subscribe(`/topic/orders/${orderId}`, (message: IMessage) => {
          try {
            const update = JSON.parse(message.body) as { status?: string };
            if (update.status) {
              onStatusChange(update.status);
            }
          } catch {}
        });
        onConnected?.();
      },
    });

    this.client = client;
    client.activate();
  }

  protected createSocket() {
    // withCredentials : sans effet en dev (proxy same-origin), mais requis en
    // prod cross-origin si l'utilisateur a un cookie JWT valide.
    return new SockJS('/ws', undefined, { withCredentials: true });
  }

  protected createClient(config: StompConfig): Client {
    return new Client(config);
  }

  disconnect(): void {
    this.subscription?.unsubscribe();
    this.subscription = null;
    void this.client?.deactivate();
    this.client = null;
  }
}
