import { useCallback, useEffect, useState } from 'react';
import Lobby from './components/Lobby';
import Room from './components/Room';
import type { Session } from './types';

const STORAGE_KEY = 'mocsar.session';

function loadSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    return null;
  }
}

export default function App() {
  const [session, setSession] = useState<Session | null>(loadSession);
  const leave = useCallback(() => setSession(null), []);

  useEffect(() => {
    try {
      if (session) localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
      else localStorage.removeItem(STORAGE_KEY);
    } catch {
      // privát ablakban nincs tárhely
    }
  }, [session]);

  return (
    <div className="app">
      <header className="app-header">
        <h1>Mocsár</h1>
        {session && (
          <span className="session-info">
            {session.nickname} · szoba <strong>{session.code}</strong>
          </span>
        )}
      </header>
      {session ? <Room session={session} onLeave={leave} /> : <Lobby onJoined={setSession} />}
    </div>
  );
}
