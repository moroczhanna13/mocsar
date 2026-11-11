import type { PlayerStats } from '../types';

export default function Leaderboard({ rows }: { rows: PlayerStats[] }) {
  if (rows.length === 0) {
    return <p className="hint">Még nincs befejezett kör.</p>;
  }
  return (
    <table className="leaderboard">
      <thead>
        <tr>
          <th>Játékos</th>
          <th>Kör</th>
          <th>Király</th>
          <th>Mocsár</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.nickname}>
            <td>{r.nickname}</td>
            <td>{r.rounds}</td>
            <td>{r.kings}</td>
            <td>{r.swamps}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
