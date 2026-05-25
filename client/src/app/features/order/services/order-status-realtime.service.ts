import { Client, IMessage, StompConfig, StompSubscription } from '@stomp/stompjs';
import { Injectable } from '@angular/core';
import SockJS from 'sockjs-client';

@Injectable({ providedIn: 'root' })
export class OrderStatusRealtimeService {
  private client: Client | null = null;
  private subscription: StompSubscription | null = null;

  connect(orderId: string, onStatusChange: (status: string) => void): void {
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
      },
    });

    this.client = client;
    client.activate();
  }

  protected createSocket() {
    return new SockJS('/ws');
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
