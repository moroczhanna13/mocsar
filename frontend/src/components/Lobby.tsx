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

  useEffect(() => {
    api.globalStats().then(setStats).catch(() => setStats([]));
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
    void run(() => api.createRoom(nickname));
  }

  function join(e: FormEvent) {
    e.preventDefault();
    void run(() => api.joinRoom(code.trim().toUpperCase(), nickname));
  }

  return (
    <main className="lobby">
      <section className="card-panel">
        <h2>Belépés</h2>
        <label>
          Becenév
          <input
            value={nickname}
            maxLength={32}
            onChange={(e) => setNickname(e.target.value)}
            placeholder="pl. Hanna"
            autoFocus
          />
        </label>
        <form onSubmit={create} className="row">
          <button type="submit" disabled={busy || !nickname.trim()}>
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
          <button type="submit" disabled={busy || !nickname.trim() || !code.trim()}>
            Csatlakozás
          </button>
        </form>
        {error && <p className="error">{error}</p>}
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
