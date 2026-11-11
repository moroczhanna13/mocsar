// A szerver DTO-inak tükre (hu.mocsar.server.api.dto).

export type Phase = 'WAITING' | 'EXCHANGE' | 'PLAYING' | 'BETWEEN_ROUNDS';

export type Role = 'KING' | 'QUEEN' | 'PRINCE' | 'CITIZEN' | 'CLEANER' | 'MAID' | 'SWAMP';

export interface PlayerInfo {
  id: string;
  nickname: string;
  bot: boolean;
  cardCount: number;
  role: Role | null;
  place: number | null;
  /** Ebben a kupacban már passzolt, a lezárásig kimarad. */
  passed: boolean;
}

export interface PileInfo {
  topRank: string | null;
  setSize: number;
  topCards: string[];
  cardCount: number;
}

export interface TransferInfo {
  from: string;
  to: string;
  count: number;
  cards: string[] | null;
}

export interface RoundSummary {
  number: number;
  finishOrder: string[];
  roles: Record<string, Role>;
}

export interface RoomView {
  roomCode: string;
  phase: Phase;
  roundNumber: number;
  me: string;
  hostId: string | null;
  canStart: boolean;
  players: PlayerInfo[];
  currentPlayer: string | null;
  hand: string[];
  pile: PileInfo | null;
  lastPlacer: string | null;
  finishOrder: string[];
  pendingGiveBack: Record<string, number>;
  myGiveBack: number;
  exchange: TransferInfo[];
  lastRound: RoundSummary | null;
}

export interface ServerMessage {
  type: 'state' | 'error';
  state: RoomView | null;
  message: string | null;
}

export interface JoinResponse {
  code: string;
  playerId: string;
  token: string;
}

export interface PlayerStats {
  nickname: string;
  rounds: number;
  kings: number;
  swamps: number;
}

export interface Session {
  code: string;
  playerId: string;
  token: string;
  nickname: string;
}

export const ROLE_LABEL: Record<Role, string> = {
  KING: 'Király',
  QUEEN: 'Királynő',
  PRINCE: 'Herceg',
  CITIZEN: 'Polgár',
  CLEANER: 'Takarítónő',
  MAID: 'Cseléd',
  SWAMP: 'Mocsár',
};

export const PHASE_LABEL: Record<Phase, string> = {
  WAITING: 'Várakozás a játékosokra',
  EXCHANGE: 'Lapcsere',
  PLAYING: 'Kör folyamatban',
  BETWEEN_ROUNDS: 'A kör véget ért',
};
