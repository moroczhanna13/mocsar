# Mocsár

Online, 7–15 fős Mocsár kártyajáték: keretrendszer-független Java szabálymotor, Spring Boot szerver
WebSocket (STOMP) állapotküldéssel, React-frontend, PostgreSQL-ben tárolt ranglista.

## Felépítés

| Modul | Feladat | Technológia |
|---|---|---|
| `mocsar-core` | Szabálymotor: lapok, osztás, lépések érvényessége, kupac, kifogyás, szerepek, lapcsere | Java 21, JUnit 5 |
| `mocsar-bot` | Gépi játékosok (`LowestCardBot`, `RandomBot`) teszteléshez és gyakorláshoz | Java 21 |
| `mocsar-server` | Szobák kóddal, STOMP WebSocket, lépések ellenőrzése, körök mentése, statisztika | Spring Boot 3.5, Spring Data JPA, PostgreSQL / H2 |
| `frontend` | Lobby, saját kéz, asztal, passz, lapcsere, szerepek, ranglista | React 18, TypeScript, Vite |

A szerver dönti el, hogy egy lépés szabályos-e. A kliens csak a saját lapjait kapja meg, másokról csak a lapszámot.

## Szabályok (a program által használt értelmezés)

- 2 pakli francia kártya joker nélkül, 104 lap. Erősorrend: 3 (leggyengébb) … 10, J, Q, K, A, 2 (legerősebb). A szín nem számít.
- Osztás: mindenki `104 / játékosszám` lapot kap, a maradékot a program véletlenszerűen kiveszi (a játékosok nem látják).
- Lépés: a kezdő 1 vagy több azonos értékű lapot tesz (pl. két K-t). A következőknek pontosan ugyanannyi lapot kell tenniük, ugyanakkora vagy nagyobb értékből (5-ösre 5…2, két K-ra két K, két A vagy két 2). Passzolni akkor is lehet, ha lenne jó lap; üres kupacra a kezdőnek tennie kell.
- Aki passzolt, abból a kupacból kimarad a lezárásig, akkor is, ha később tudna tenni.
- A kupac lezárul, ha a legutóbbi lerakón kívül már senki sem tehet (mindenki más passzolt vagy kifogyott). Az új kupacot a legutóbbi lerakó kezdi, ha neki elfogyott a lapja, a sorban utána következő.
- Nincs kupaczáró lap (a 2-es sem).
- Kifogyási sorrend adja a szerepeket: király, királynő, herceg, polgárok, takarítónő, cseléd, Mocsár.
- Következő kör: ülésrend = szerepek sorrendje. Lapcsere: takarítónő 1, cseléd 2, Mocsár 3 legjobb lapját adja fel (a 2-es megelőzi az ászt); a herceg, királynő, király ugyanannyi tetszőleges lapot ad vissza. A király kezd.
- Első körben sorsolt játékos kezd, az ülésrend a csatlakozás sorrendje.
- Csatlakozni és kilépni csak két kör között lehet. Az új játékos polgár lesz (a polgárok végére ül). Ha valaki kilép, a többiek szerepe feljebb csúszik.
- Statisztika: ki hányszor volt király, hányszor Mocsár (szobánként és összesítve).

## Futtatás

### Fejlesztői mód (Docker nélkül, H2 memória-adatbázis)

Adatbázis env-ből: `DB_URL`, `DB_USER`, `DB_PASSWORD` (nélkülük H2). Port: `PORT` (alap 8080). Engedélyezett frontend-origin: `MOCSAR_ALLOWED_ORIGINS`.

```
mvn -q install -DskipTests
cd mocsar-server && mvn spring-boot:run
```

Másik terminálban:

```
cd frontend
npm install
npm run dev
```

Ezután `http://localhost:5173`. A Vite a `/api` és `/ws` útvonalakat a 8080-as szerverre továbbítja.

Egyedül is lehet próbálni: szoba létrehozása után a gazda a „Bot hozzáadása” gombbal ültet be gépi játékosokat.

### Egy paranccsal (Docker Compose, PostgreSQL)

```
docker compose up --build
```

Frontend: `http://localhost:3000`, szerver: `http://localhost:8080`, adatbázis a `db` szolgáltatásban.

### Ingyenes online: GitHub Pages + Render

- Frontend: `https://moroczhanna13.github.io/mocsar/` (a [pages.yml](.github/workflows/pages.yml) minden `main`-re pusholt frontend-változásnál buildel és deployol). Repó Settings > Pages > Source: **GitHub Actions**.
- Szerver: Render.com, New > **Blueprint**, ez a repó; a [render.yaml](render.yaml) ingyenes Docker web service-t hoz létre `mocsar-server` néven (`https://mocsar-server.onrender.com`). Ha Render más nevet ad, a repó Settings > Secrets and variables > Actions > Variables alatt `SERVER_URL` legyen az új cím, és a Pages workflow-t újra kell futtatni.
- Az ingyenes Render 15 perc HTTP-tétlenség után elaltatja a szervert; az első belépés ébreszti, a lobby ezt jelzi. Játék közben a kliens 4 percenként pingel, hogy ébren tartsa.
- A zajló játékok a szerver memóriájában élnek: újraindítás vagy újratelepítés („Nincs ilyen szoba”) elveszíti őket. A `render.yaml` `buildFilter`-e miatt csak backend-változás indít újratelepítést; ne pusholj backend-változást, amíg mások játszanak.
- Tartós ranglista: ingyenes PostgreSQL (pl. [Neon](https://neon.tech)) adatait a Render dashboardon env-ként: `DB_URL=jdbc:postgresql://<host>/<db>?sslmode=require`, `DB_USER`, `DB_PASSWORD`. Nélkülük H2 memória-adatbázis fut.

### Tesztek

```
mvn verify
```

- `mocsar-core`: osztás 7–15 főre, erősorrend, érvényes lépés, kupac lezárása, szerepek, lapcsere, több ezer véletlen kör (a játék sosem akad el, lap sosem keletkezik vagy tűnik el).
- `mocsar-bot`: bot-szimulációk 7–15 fővel, csatlakozás/kilépés körök között.
- `mocsar-server`: szabálytalan lépés visszautasítása, STOMP-on át lejátszott két kör két emberrel és öt bottal (egyetlen üzenet sem tartalmaz idegen lapot), körök mentése és ranglista H2-vel; ugyanez PostgreSQL-lel Testcontainers alatt (Docker nélkül a teszt kimarad).

## API röviden

REST:

- `POST /api/rooms` `{nickname}` → `{code, playerId, token}`
- `POST /api/rooms/{code}/join` `{nickname}`
- `GET /api/rooms/{code}` nyilvános összefoglaló
- `GET /api/rooms/{code}/me` (fejléc: `X-Player-Token`) saját nézet
- `POST /api/rooms/{code}/bots`, `/leave`, `/end` (fejléc: `X-Player-Token`)
- `GET /api/rooms/{code}/stats`, `GET /api/stats` ranglista

STOMP (`/ws`):

- küldés: `/app/rooms/{code}/{sync|start|play|pass|giveBack|addBot|leave}` törzs: `{token, cards}`
- fogadás: `/topic/rooms/{code}/players/{token}` törzs: `{type: "state"|"error", state, message}`

A token a játékos titka: ezzel lép, és csak erre a témára kapja meg a saját lapjait.

## Adatmodell

`game` (id, room_code, status, created_at), `player` (id, nickname, game_id, join_order, bot),
`round` (id, game_id, number, player_count, removed_cards, finished_at),
`round_result` (round_id, player_id, place, role). A zajló kör állapota csak a szerver memóriájában él.
