import type { RoomView } from '../types';
import { ROLE_LABEL } from '../types';

export default function Players({ view }: { view: RoomView }) {
  return (
    <ul className="players">
      {view.players.map((p) => {
        const classes = ['player'];
        if (p.id === view.currentPlayer) classes.push('current');
        if (p.id === view.me) classes.push('me');
        if (p.place) classes.push('finished');
        if (p.passed) classes.push('passed');
        return (
          <li key={p.id} className={classes.join(' ')}>
            <span className="name">
              {p.nickname}
              {p.bot && <span className="tag">bot</span>}
              {p.id === view.hostId && <span className="tag">gazda</span>}
            </span>
            <span className="meta">
              {p.role && <span className="role">{ROLE_LABEL[p.role]}</span>}
              {p.place ? <span className="place">{p.place}. hely</span> : <span className="count">{p.cardCount} lap</span>}
              {p.passed && <span className="tag">passzolt</span>}
              {view.pendingGiveBack[p.id] ? <span className="give">{view.pendingGiveBack[p.id]} lapot ad vissza</span> : null}
            </span>
          </li>
        );
      })}
    </ul>
  );
}
