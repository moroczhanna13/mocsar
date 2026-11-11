import { FormEvent, useEffect, useState } from 'react';
import { api } from '../api';
import type { PlayerStats, Session } from '../types';
import Leaderboard from './Leaderboard';
import Rules from './Rules';

interface Props {
  onJoined(session: Session): void;
}

export default function Lobby({ onJoined }: Props) {
  const [nickname, setNickname] = useState('');
  const [code, setCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [stats, setStats] = useState<PlayerStats[]>([]);
  const [rulesOpen, setRulesOpen] = useState(false);
  const [server, setServer] = useState<'waking' | 'up' | 'down'>('waking');

  // Az ingyenes hoszton a szerver tétlenség után elalszik; az első kérés felébreszti (akár 1 perc).
  useEffect(() => {
    let cancelled = false;
    const attempt = (left: number) => {
      api
        .globalStats()
        .then((rows) => {
          if (cancelled) return;
          setStats(rows);
          setServer('up');
        })
        .catch(() => {
          if (cancelled) return;
          if (left > 0) setTimeout(() => attempt(left - 1), 5000);
          else setServer('down');
        });
    };
    attempt(18);
    return () => {
      cancelled = true;
    };
  }, []);

  async function run(action: () => Promise<{ code: string; playerId: string; token: string }>) {
    setBusy(true);
    setError(null);
    try {
      const joined = await action();
      onJoined({ ...joined, nickname: nickname.trim() });
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }

  function create(e: FormEvent) {
    e.preventDefault();
    if (!nickname.trim()) {
      setError('Először add meg a beceneved.');
      return;
    }
    void run(() => api.createRoom(nickname));
  }

  function join(e: FormEvent) {
    e.preventDefault();
    if (!nickname.trim()) {
      setError('Először add meg a beceneved.');
      return;
    }
    if (!code.trim()) {
      setError('Írd be a szobakódot, amit a szoba gazdája kapott.');
      return;
    }
    void run(() => api.joinRoom(code.trim().toUpperCase(), nickname));
  }

  return (
    <main className="lobby">
      <section className="card-panel">
        <h2>Belépés</h2>
        <label>
          Becenév (kötelező, a szobában egyedi)
          <input
            value={nickname}
            maxLength={32}
            onChange={(e) => setNickname(e.target.value)}
            placeholder="pl. Hanna"
            autoFocus
          />
        </label>
        <form onSubmit={create} className="row">
          <button type="submit" disabled={busy}>
            Új szoba
          </button>
        </form>
        <form onSubmit={join} className="row">
          <input
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            placeholder="Szobakód"
            maxLength={8}
          />
          <button type="submit" disabled={busy}>
            Csatlakozás
          </button>
        </form>
        {error && <p className="error">{error}</p>}
        {server === 'waking' && <p className="hint">A szerver ébredezik, ez az ingyenes hoszton akár egy percig is eltarthat…</p>}
        {server === 'down' && <p className="error">A szerver nem érhető el. Próbáld újra később.</p>}
        <p className="hint">7–15 játékos, két pakli, joker nélkül. A hiányzó helyekre botot is lehet ültetni.</p>
        <button type="button" className="secondary" onClick={() => setRulesOpen(true)}>
          Szabályok
        </button>
        <Rules open={rulesOpen} onClose={() => setRulesOpen(false)} />
      </section>
      <section className="card-panel">
        <h2>Ranglista</h2>
        <Leaderboard rows={stats} />
      </section>
    </main>
  );
}
