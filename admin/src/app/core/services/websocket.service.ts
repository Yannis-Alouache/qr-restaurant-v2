import { Client, IMessage, StompConfig, StompSubscription } from '@stomp/stompjs';
import { Injectable, inject, signal } from '@angular/core';
import SockJS from 'sockjs-client';
import { RestaurantService } from './restaurant.service';

export interface WebSocketMessage {
  orderId: string;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private restaurant = inject(RestaurantService);

  private client: Client | null = null;
  private subscription: StompSubscription | null = null;
  connected = signal(false);

  connect(onMessage: (msg: WebSocketMessage) => void): void {
    const restaurant = this.restaurant.restaurant();
    if (!restaurant || this.client?.active) {
      return;
    }

    const client = this.createClient({
      webSocketFactory: () => this.createSocket(),
      reconnectDelay: 5000,
      onConnect: () => {
        this.connected.set(true);
        this.subscription?.unsubscribe();
        this.subscription = client.subscribe(`/topic/restaurants/${restaurant.id}/orders`, (message: IMessage) => {
          try {
            onMessage(JSON.parse(message.body) as WebSocketMessage);
          } catch {}
        });
      },
      onStompError: () => {
        this.connected.set(false);
      },
      onWebSocketClose: () => {
        this.connected.set(false);
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
    this.connected.set(false);
  }
}
