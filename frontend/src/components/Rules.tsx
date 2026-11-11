import { useEffect, useRef } from 'react';

interface Props {
  open: boolean;
  onClose(): void;
}

/** A játékszabályok felugró ablakban. */
export default function Rules({ open, onClose }: Props) {
  const dialog = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const el = dialog.current;
    if (!el) return;
    if (open && !el.open) el.showModal();
    if (!open && el.open) el.close();
  }, [open]);

  return (
    <dialog ref={dialog} className="rules" onClose={onClose} onClick={(e) => e.target === dialog.current && onClose()}>
      <div className="rules-body">
        <h2>A Mocsár szabályai</h2>
        <h3>Alapok</h3>
        <ul>
          <li>7–15 játékos, két pakli francia kártya joker nélkül (104 lap).</li>
          <li>
            Erősorrend gyengétől erősig: <strong>3, 4, 5, 6, 7, 8, 9, 10, J, Q, K, A, 2</strong>. A szín nem számít.
          </li>
          <li>Mindenki ugyanannyi lapot kap. Ami nem osztható el egyenlően, azt a program kiveszi; senki nem látja, melyek azok.</li>
        </ul>
        <h3>Egy kupac</h3>
        <ul>
          <li>A kezdő letesz egy lapot, vagy több azonos értékűt (pl. két K-t). Minél kisebbel érdemes kezdeni.</li>
          <li>
            A következőnek <strong>ugyanannyi</strong> lapot kell tennie, <strong>ugyanakkora vagy nagyobb</strong> értékből.
            Példa: 5-ösre 5, 6, 7, …, A vagy 2 mehet; két K-ra két K, két A vagy két 2.
          </li>
          <li>Passzolni akkor is lehet, ha lenne jó lap. Aki passzolt, abból a kupacból kimarad a lezárásig.</li>
          <li>
            A kupac lezárul, ha a legutóbbi lerakón kívül már senki sem tehet. Az új kupacot a legutóbbi lerakó kezdi;
            ha neki elfogyott a lapja, a sorban utána következő.
          </li>
        </ul>
        <h3>Kör vége és szerepek</h3>
        <ul>
          <li>Akinek először elfogy a lapja, az nyeri a kört. A kifogyás sorrendje adja a szerepeket:</li>
          <li>
            <strong>Király</strong>, <strong>Királynő</strong>, <strong>Herceg</strong>, polgárok (minden köztes hely),{' '}
            <strong>Takarítónő</strong>, <strong>Cseléd</strong>, <strong>Mocsár</strong> (az utolsó).
          </li>
        </ul>
        <h3>Következő kör</h3>
        <ul>
          <li>A játékosok a szerepek sorrendjében ülnek, ez a lépéssorrend is. A király kezd.</li>
          <li>
            Lapcsere osztás után: a takarítónő a legjobb lapját a hercegnek adja, a cseléd a két legjobbat a királynőnek,
            a Mocsár a három legjobbat a királynak. A herceg, a királynő és a király ugyanannyi tetszőleges lapot ad vissza.
          </li>
          <li>Csatlakozni és kilépni csak két kör között lehet; az új játékos polgár lesz.</li>
          <li>A játék addig megy, amíg akarjátok. A ranglista azt mutatja, ki hányszor volt király és Mocsár.</li>
        </ul>
        <div className="row">
          <button type="button" onClick={onClose}>
            Bezárás
          </button>
        </div>
      </div>
    </dialog>
  );
}
