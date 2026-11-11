import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api } from '../api';
import { parseCard, strengthOf } from '../cards';
import { connect, type GameSocket } from '../socket';
import type { PlayerStats, RoomView, ServerMessage, Session } from '../types';
import { PHASE_LABEL, ROLE_LABEL } from '../types';
import CardView from './CardView';
import Hand from './Hand';
import Leaderboard from './Leaderboard';
import Players from './Players';

interface Props {
  session: Session;
  onLeave(): void;
}

export default function Room({ session, onLeave }: Props) {
  const [view, setView] = useState<RoomView | null>(null);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string[]>([]);
  const [stats, setStats] = useState<PlayerStats[]>([]);
  const [roomLost, setRoomLost] = useState(false);
  const socket = useRef<GameSocket | null>(null);

  const handleMessage = useCallback(
    (msg: ServerMessage) => {
      if (msg.type === 'state' && msg.state) {
        setView(msg.state);
        setError(null);
      } else if (msg.type === 'error' && msg.message) {
        if (msg.message === 'A játék véget ért' || msg.message === 'Kiléptél a szobából') {
          onLeave();
          return;
        }
        if (msg.message.startsWith('Nincs ilyen szoba') || msg.message.startsWith('Érvénytelen játékos')) {
          setRoomLost(true);
          return;
        }
        setError(msg.message);
      }
    },
    [onLeave],
  );

  // Az ingyenes hoszt HTTP-forgalom nélkül elaltatja a szervert; a WebSocket nem számít. 4 percenként ping.
  useEffect(() => {
    const timer = setInterval(() => api.ping().catch(() => undefined), 4 * 60 * 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    socket.current = connect(session.code, session.token, handleMessage, setConnected);
    // ha a szoba már nem él (pl. szerver újraindult), vissza a lobbyba
    api
      .me(session.code, session.token)
      .then(setView)
      .catch((e: Error) => {
        if (e.message.startsWith('Nincs ilyen szoba') || e.message.startsWith('Érvénytelen')) setRoomLost(true);
        else setError(e.message);
      });
    return () => socket.current?.close();
  }, [session.code, session.token, handleMessage, onLeave]);

  // a kijelölés ne ragadjon be fázisváltásnál
  useEffect(() => {
    setSelected([]);
  }, [view?.phase, view?.roundNumber, view?.currentPlayer]);

  useEffect(() => {
    if (view?.phase === 'BETWEEN_ROUNDS' || view?.phase === 'WAITING') {
      api.roomStats(session.code).then(setStats).catch(() => undefined);
    }
  }, [view?.phase, view?.roundNumber, session.code]);

  const send = (action: Parameters<GameSocket['send']>[0], cards?: string[]) => socket.current?.send(action, cards);

  const isHost = view?.hostId === view?.me;
  const myTurn = view?.phase === 'PLAYING' && view.currentPlayer === view.me;
  const pile = view?.pile ?? null;
  const pileEmpty = !pile || pile.topRank === null;

  /** Lerakásnál csak a kupacra tehető lapok választhatók. */
  const selectable = useMemo(() => {
    if (!view) return null;
    if (view.phase === 'EXCHANGE' && view.myGiveBack > 0) return () => true;
    if (!myTurn) return () => false;
    if (pileEmpty) return () => true;
    const top = strengthOf(pile!.topRank!);
    const counts = new Map<string, number>();
    for (const id of view.hand) {
      const r = parseCard(id).rank;
      counts.set(r, (counts.get(r) ?? 0) + 1);
    }
    return (id: string) => {
      const c = parseCard(id);
      return c.strength >= top && (counts.get(c.rank) ?? 0) >= pile!.setSize;
    };
  }, [view, myTurn, pile, pileEmpty]);

  const needed = view?.phase === 'EXCHANGE' ? view.myGiveBack : pileEmpty ? null : pile!.setSize;

  /**
   * Kijelölés: kattintásra ki/be. Lerakásnál csak azonos értékű lapok mehetnek együtt, ezért
   * más értékre kattintva az új lap átveszi a kijelölést. Ha már megvan a kötelező darabszám,
   * az újabb lap a legrégebbi helyére lép. Lapcserénél bármilyen lapok mehetnek együtt.
   */
  function toggle(id: string) {
    setSelected((s) => {
      if (s.includes(id)) return s.filter((x) => x !== id);
      const playing = view?.phase === 'PLAYING';
      if (playing && s.length > 0 && parseCard(s[0]).rank !== parseCard(id).rank) return [id];
      if (needed !== null && needed > 0 && s.length >= needed) return [...s.slice(1), id];
      return [...s, id];
    });
  }
  const canPlay = myTurn && selected.length > 0 && (needed === null || selected.length === needed);
  const canGiveBack = view?.phase === 'EXCHANGE' && view.myGiveBack > 0 && selected.length === view.myGiveBack;

  async function leave() {
    try {
      await api.leave(session.code, session.token);
    } catch (e) {
      setError((e as Error).message);
      return;
    }
    onLeave();
  }

  async function endGame() {
    if (!confirm('Biztosan lezárod a játékot mindenkinek?')) return;
    try {
      await api.endGame(session.code, session.token);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  if (roomLost) {
    return (
      <main className="room">
        <section className="card-panel">
          <h2>A szoba megszűnt</h2>
          <p>
            A szerver újraindult, és a zajló játékok csak a memóriájában éltek. A befejezett körök eredménye megmaradt
            a ranglistán. Hozzatok létre új szobát.
          </p>
          <button onClick={onLeave}>Vissza a lobbyba</button>
        </section>
      </main>
    );
  }

  if (!view) {
    return (
      <main className="room">
        <p className="hint">{connected ? 'Állapot betöltése…' : 'Kapcsolódás…'}</p>
        <button onClick={onLeave}>Vissza a lobbyba</button>
      </main>
    );
  }

  const nick = (id: string | null) => view.players.find((p) => p.id === id)?.nickname ?? id ?? '';
  const myRole = view.players.find((p) => p.id === view.me)?.role ?? null;

  return (
    <main className="room">
      <section className="status">
        <div>
          <strong>{PHASE_LABEL[view.phase]}</strong>
          <span className="hint"> · {view.lastRound ? `${view.lastRound.number}. kör lezárva` : '1. kör'}</span>
          {!connected && <span className="offline"> · nincs kapcsolat</span>}
        </div>
        <div className="row">
          {isHost && (view.phase === 'WAITING' || view.phase === 'BETWEEN_ROUNDS') && (
            <>
              <button onClick={() => send('start')} disabled={!view.canStart} title="Legalább 7 játékos kell">
                {view.lastRound ? 'Következő kör' : 'Kör indítása'}
              </button>
              <button onClick={() => send('addBot')} disabled={view.players.length >= 15}>
                Bot hozzáadása
              </button>
              <button className="secondary" onClick={endGame}>
                Játék lezárása
              </button>
            </>
          )}
          {(view.phase === 'WAITING' || view.phase === 'BETWEEN_ROUNDS') && (
            <button className="secondary" onClick={leave}>
              Kilépés
            </button>
          )}
        </div>
      </section>

      {error && <p className="error">{error}</p>}

      <div className="table-layout">
        <section className="card-panel">
          <h2>Játékosok ({view.players.length})</h2>
          <Players view={view} />
          {view.players.length < 7 && <p className="hint">Még {7 - view.players.length} játékos hiányzik.</p>}
        </section>

        <section className="card-panel table">
          <h2>Asztal</h2>
          {view.phase === 'PLAYING' && (
            <>
              <div className="pile">
                {pileEmpty ? (
                  <p className="hint">Üres kupac: {nick(view.currentPlayer)} kezd, bármennyi azonos lappal.</p>
                ) : (
                  <>
                    <div className="pile-cards">
                      {pile!.topCards.map((id) => (
                        <CardView key={id} id={id} small />
                      ))}
                    </div>
                    <p className="hint">
                      {nick(view.lastPlacer)} tette · {pile!.setSize} lapot kell tenni, legalább {pile!.topRank}-t ·{' '}
                      {pile!.cardCount} lap a kupacon
                    </p>
                  </>
                )}
              </div>
              <p className={myTurn ? 'turn mine' : 'turn'}>
                {myTurn ? 'Te jössz!' : `${nick(view.currentPlayer)} következik`}
              </p>
            </>
          )}
          {view.phase === 'EXCHANGE' && (
            <div className="exchange">
              {myRole && <p>Szereped: <strong>{ROLE_LABEL[myRole]}</strong></p>}
              {view.exchange.map((t, i) => (
                <p key={i} className="hint">
                  {nick(t.from)} → {nick(t.to)}: {t.count} lap{t.cards ? ` (${t.cards.map((c) => parseCard(c).rank).join(', ')})` : ''}
                </p>
              ))}
              {view.myGiveBack > 0 ? (
                <p className="turn mine">Adj vissza {view.myGiveBack} tetszőleges lapot!</p>
              ) : (
                <p className="hint">Várunk, amíg a király, a királynő és a herceg visszaad.</p>
              )}
            </div>
          )}
          {view.phase === 'BETWEEN_ROUNDS' && view.lastRound && (
            <ol className="result">
              {view.lastRound.finishOrder.map((id) => (
                <li key={id}>
                  {nick(id)} <span className="role">{ROLE_LABEL[view.lastRound!.roles[id]]}</span>
                </li>
              ))}
            </ol>
          )}
          {view.phase === 'WAITING' && <p className="hint">A gazda indítja a kört, ha összejött legalább 7 játékos.</p>}
        </section>
      </div>

      {(view.phase === 'PLAYING' || view.phase === 'EXCHANGE') && (
        <section className="card-panel">
          <h2>
            Kezed ({view.hand.length} lap)
            {myRole && view.phase === 'PLAYING' && <span className="role"> {ROLE_LABEL[myRole]}</span>}
          </h2>
          <Hand cards={view.hand} selected={selected} selectable={selectable} onToggle={toggle} />
          {myTurn && (
            <p className="hint">
              {pileEmpty
                ? 'Te kezded a kupacot: tegyél 1 vagy több azonos értékű lapot. A többieknek ugyanennyit kell tenniük.'
                : `Tegyél pontosan ${pile!.setSize} lapot ${pile!.setSize > 1 ? 'azonos értékből, ' : ''}legalább ${pile!.topRank} erősségűt, vagy passzolj.`}
            </p>
          )}
          <div className="row actions">
            {view.phase === 'PLAYING' && (
              <>
                <button
                  onClick={() => {
                    send('play', selected);
                    setSelected([]);
                  }}
                  disabled={!canPlay}
                >
                  Lerakás {selected.length > 0 ? `(${selected.length})` : ''}
                </button>
                <button className="secondary" onClick={() => send('pass')} disabled={!myTurn || pileEmpty}>
                  Passz
                </button>
              </>
            )}
            {view.phase === 'EXCHANGE' && (
              <button
                onClick={() => {
                  send('giveBack', selected);
                  setSelected([]);
                }}
                disabled={!canGiveBack}
              >
                Visszaadás ({selected.length}/{view.myGiveBack})
              </button>
            )}
          </div>
        </section>
      )}

      {(view.phase === 'BETWEEN_ROUNDS' || view.phase === 'WAITING') && (
        <section className="card-panel">
          <h2>Ranglista (ez a szoba)</h2>
          <Leaderboard rows={stats} />
        </section>
      )}
    </main>
  );
}
