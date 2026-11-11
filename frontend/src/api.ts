import type { JoinResponse, PlayerStats, RoomView } from './types';

const BASE = import.meta.env.VITE_API_URL ?? '';

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(BASE + path, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init.headers ?? {}) },
  });
  if (!response.ok) {
    let message = `Hiba (${response.status})`;
    try {
      const body = await response.json();
      if (body?.message) message = body.message;
    } catch {
      // nincs JSON törzs
    }
    throw new Error(message);
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export const api = {
  createRoom: (nickname: string) =>
    request<JoinResponse>('/api/rooms', { method: 'POST', body: JSON.stringify({ nickname }) }),
  joinRoom: (code: string, nickname: string) =>
    request<JoinResponse>(`/api/rooms/${code}/join`, { method: 'POST', body: JSON.stringify({ nickname }) }),
  me: (code: string, token: string) =>
    request<RoomView>(`/api/rooms/${code}/me`, { headers: { 'X-Player-Token': token } }),
  addBot: (code: string, token: string) =>
    request<unknown>(`/api/rooms/${code}/bots`, { method: 'POST', headers: { 'X-Player-Token': token } }),
  leave: (code: string, token: string) =>
    request<void>(`/api/rooms/${code}/leave`, { method: 'POST', headers: { 'X-Player-Token': token } }),
  endGame: (code: string, token: string) =>
    request<void>(`/api/rooms/${code}/end`, { method: 'POST', headers: { 'X-Player-Token': token } }),
  roomStats: (code: string) => request<PlayerStats[]>(`/api/rooms/${code}/stats`),
  /** Könnyű HTTP-kérés, hogy az ingyenes hoszt ne altassa el a szervert játék közben. */
  ping: () => fetch(BASE + '/actuator/health').then(() => undefined),
  globalStats: () => request<PlayerStats[]>('/api/stats'),
};
