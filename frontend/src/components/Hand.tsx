import { sortIds } from '../cards';
import CardView from './CardView';

interface Props {
  cards: string[];
  selected: string[];
  /** Melyik lapok tehetők most; null = mind. */
  selectable: ((id: string) => boolean) | null;
  onToggle(id: string): void;
}

/**
 * A kéz rendezve. Csak a szabályosan tehető lapok kattinthatók; hogy mi kerül a kijelölésbe
 * (azonos érték, darabszám), azt a szülő {@code onToggle} dönti el.
 */
export default function Hand({ cards, selected, selectable, onToggle }: Props) {
  const sorted = sortIds(cards);
  return (
    <div className="hand">
      {sorted.map((id) => {
        const isSelected = selected.includes(id);
        const clickable = isSelected || (selectable ? selectable(id) : true);
        return (
          <CardView
            key={id}
            id={id}
            selected={isSelected}
            disabled={!clickable}
            onClick={clickable ? () => onToggle(id) : undefined}
          />
        );
      })}
      {cards.length === 0 && <p className="hint">Nincs lap a kezedben.</p>}
    </div>
  );
}
