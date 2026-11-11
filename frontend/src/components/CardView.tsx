import { parseCard, SUIT_SYMBOL } from '../cards';

interface Props {
  id: string;
  selected?: boolean;
  disabled?: boolean;
  small?: boolean;
  onClick?(): void;
}

export default function CardView({ id, selected, disabled, small, onClick }: Props) {
  const card = parseCard(id);
  const classes = ['card', card.red ? 'red' : 'black'];
  if (selected) classes.push('selected');
  if (disabled) classes.push('disabled');
  if (small) classes.push('small');
  if (onClick) classes.push('clickable');
  return (
    <button type="button" className={classes.join(' ')} onClick={onClick} disabled={!onClick}>
      <span className="rank">{card.rank}</span>
      <span className="suit">{SUIT_SYMBOL[card.suit]}</span>
    </button>
  );
}
