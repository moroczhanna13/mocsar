import { Client } from '@stomp/stompjs';
import type { ServerMessage } from './types';

export type Action = 'sync' | 'start' | 'play' | 'pass' | 'giveBack' | 'addBot' | 'leave';

export interface GameSocket {
  send(action: Action, cards?: string[]): void;
  close(): void;
}

function wsUrl(): string {
  const configured = import.meta.env.VITE_WS_URL as string | undefined;
  if (configured) return configured;
  // ha a REST máshol fut (pl. GitHub Pages + Render), a WebSocket is oda megy
  const api = import.meta.env.VITE_API_URL as string | undefined;
  if (api) return api.replace(/^http/, 'ws').replace(/\/$/, '') + '/ws';
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
  return `${protocol}://${location.host}/ws`;
}

/**
 * STOMP-kapcsolat egy szobához. A játékos a saját privát témájára iratkozik fel,
 * ezen jön minden állapot és hiba. Lépéseket /app/rooms/{code}/{action}-re küld.
 */
export function connect(
  code: string,
  token: string,
  onMessage: (msg: ServerMessage) => void,
  onConnection: (connected: boolean) => void,
): GameSocket {
  const client = new Client({
    brokerURL: wsUrl(),
    reconnectDelay: 2000,
    onConnect: () => {
      onConnection(true);
      client.subscribe(`/topic/rooms/${code}/players/${token}`, (frame) => {
        onMessage(JSON.parse(frame.body) as ServerMessage);
      });
      send('sync');
    },
    onDisconnect: () => onConnection(false),
    onWebSocketClose: () => onConnection(false),
  });

  function send(action: Action, cards?: string[]) {
    if (!client.connected) return;
    client.publish({
      destination: `/app/rooms/${code}/${action}`,
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ token, cards: cards ?? null }),
    });
  }

  client.activate();
  return {
    send,
    close: () => {
      void client.deactivate();
    },
  };
}
