// Lapazonosító: <érték><szín>-<copy>, pl. "10H-1". Az erősorrend: 3 a leggyengébb, 2 a legerősebb.

export const RANKS = ['3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K', 'A', '2'] as const;

export const SUIT_SYMBOL: Record<string, string> = { C: '♣', D: '♦', H: '♥', S: '♠' };

export interface ParsedCard {
  id: string;
  rank: string;
  suit: string;
  strength: number;
  red: boolean;
}

export function parseCard(id: string): ParsedCard {
  const dash = id.lastIndexOf('-');
  const left = id.substring(0, dash);
  const suit = left.charAt(left.length - 1);
  const rank = left.substring(0, left.length - 1);
  return { id, rank, suit, strength: RANKS.indexOf(rank as (typeof RANKS)[number]), red: suit === 'H' || suit === 'D' };
}

export function strengthOf(rankLabel: string): number {
  return RANKS.indexOf(rankLabel as (typeof RANKS)[number]);
}

export function sortIds(ids: string[]): string[] {
  return [...ids].sort((a, b) => {
    const pa = parseCard(a);
    const pb = parseCard(b);
    return pa.strength - pb.strength || pa.suit.localeCompare(pb.suit) || a.localeCompare(b);
  });
}
