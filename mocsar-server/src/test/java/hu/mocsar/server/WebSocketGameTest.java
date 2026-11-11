package hu.mocsar.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.mocsar.core.Game;
import hu.mocsar.server.api.dto.ActionMessage;
import hu.mocsar.server.api.dto.JoinRequest;
import hu.mocsar.server.api.dto.JoinResponse;
import hu.mocsar.server.api.dto.RoomView;
import hu.mocsar.server.persistence.PlayerStats;
import hu.mocsar.server.room.RoomService;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.util.MimeTypeUtils;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * Két ember és öt bot STOMP-on át lejátszik két kört. Ellenőrzi, hogy a szerver a szabálytalan
 * lépést visszautasítja, és egyetlen üzenetben sem küld a kliensnek más lapjairól adatot.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "mocsar.bot-delay-ms=0")
class WebSocketGameTest {

    private static final Pattern CARD_ID = Pattern.compile("\\b(?:10|[2-9JQKA])[CDHS]-[01]\\b");

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    final ObjectMapper json = new ObjectMapper();

    /** Egy STOMP-kliens egy játékoshoz: minden beérkező nyers üzenetet eltesz. */
    class Client {
        final String code;
        final JoinResponse me;
        final StompSession session;
        final BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
        final List<String> allPayloads = new ArrayList<>();
        RoomView last;

        Client(String code, JoinResponse me) throws Exception {
            this.code = code;
            this.me = me;
            WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
            client.setMessageConverter(new SimpleMessageConverter());
            session = client.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
            }).get(5, TimeUnit.SECONDS);
            session.subscribe(RoomService.topicFor(code, me.token()), new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return byte[].class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    inbox.add(new String((byte[]) payload, StandardCharsets.UTF_8));
                }
            });
            Thread.sleep(200);
            send("sync", null);
        }

        void send(String action, List<String> cards) {
            StompHeaders headers = new StompHeaders();
            headers.setDestination("/app/rooms/" + code + "/" + action);
            headers.setContentType(MimeTypeUtils.APPLICATION_JSON);
            session.send(headers, toJson(new ActionMessage(me.token(), cards)).getBytes(StandardCharsets.UTF_8));
        }

        /** A következő üzenet; állapot esetén frissíti a {@code last} nézetet. */
        JsonNode next(long millis) throws Exception {
            String payload = inbox.poll(millis, TimeUnit.MILLISECONDS);
            if (payload == null) {
                return null;
            }
            allPayloads.add(payload);
            JsonNode node = json.readTree(payload);
            if ("state".equals(node.path("type").asText())) {
                last = json.treeToValue(node.get("state"), RoomView.class);
            }
            return node;
        }

        JsonNode awaitError(String fragment) throws Exception {
            long deadline = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < deadline) {
                JsonNode node = next(200);
                if (node != null && "error".equals(node.path("type").asText())
                        && node.path("message").asText().contains(fragment)) {
                    return node;
                }
            }
            throw new AssertionError("Nem jött hibaüzenet: " + fragment);
        }

        /** Egy lépés, ha rajtam a sor; igaz, ha a megadott kör véget ért. */
        boolean step(int round) throws Exception {
            JsonNode node = next(50);
            if (node == null || last == null) {
                return false;
            }
            if (last.phase() == Game.Phase.BETWEEN_ROUNDS) {
                return last.lastRound() != null && last.lastRound().number() == round;
            }
            actOnLast();
            return false;
        }

        /** Az utolsó ismert állapot szerint lép, ha rajtam a sor vagy lapot kell visszaadnom. */
        void actOnLast() {
            if (last == null) {
                return;
            }
            if (last.phase() == Game.Phase.EXCHANGE && last.myGiveBack() > 0) {
                send("giveBack", TestMoves.weakest(last, last.myGiveBack()));
            } else if (last.phase() == Game.Phase.PLAYING && last.me().equals(last.currentPlayer())) {
                Optional<List<String>> play = TestMoves.lowestPlay(last);
                if (play.isPresent()) {
                    send("play", play.get());
                } else {
                    send("pass", null);
                }
            }
        }
    }

    private String toJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JoinResponse post(String path, String nickname) {
        ResponseEntity<JoinResponse> response = rest.postForEntity(path, new JoinRequest(nickname), JoinResponse.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).as(path).isTrue();
        return response.getBody();
    }

    private HttpHeaders tokenHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Player-Token", token);
        return headers;
    }

    @Test
    void twoHumansAndFiveBotsPlayTwoRoundsOverStomp() throws Exception {
        JoinResponse anna = post("/api/rooms", "Anna");
        String code = anna.code();
        JoinResponse bence = post("/api/rooms/" + code + "/join", "Bence");
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> r = rest.exchange("/api/rooms/" + code + "/bots", HttpMethod.POST,
                    new HttpEntity<>(tokenHeader(anna.token())), String.class);
            assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        }

        Client a = new Client(code, anna);
        Client b = new Client(code, bence);

        // szabálytalan: kör előtt nem lehet lépni, vendég nem indíthat
        a.send("play", List.of("3C-0"));
        a.awaitError("Most nem lehet lépni");
        b.send("start", null);
        b.awaitError("gazdája");

        a.send("start", null);
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline && (a.last == null || a.last.phase() != Game.Phase.PLAYING)) {
            a.next(100);
        }
        assertThat(a.last.phase()).isEqualTo(Game.Phase.PLAYING);
        assertThat(a.last.hand()).hasSize(14);

        // szabálytalan: nem a saját lap, vagy nem ő jön
        String foreign = a.last.hand().contains("3C-0") ? "3C-1" : "3C-0";
        a.send("play", List.of(foreign));
        JsonNode rejected = a.awaitError("");
        assertThat(rejected.path("message").asText()).isIn("Nem te következel", "Nincs nálad ez a lap");
        // a hibára várva elnyelt állapotok szerint lépünk, különben a játék ránk várna
        a.actOnLast();
        b.actOnLast();

        for (int round = 1; round <= 2; round++) {
            deadline = System.currentTimeMillis() + 60_000;
            boolean aDone = false;
            boolean bDone = false;
            while ((!aDone || !bDone) && System.currentTimeMillis() < deadline) {
                aDone |= a.step(round);
                bDone |= b.step(round);
            }
            assertThat(aDone && bDone)
                    .as("a(z) " + round + ". kör véget ért; A: " + a.last + " | utolsó üzenetek A: "
                            + a.allPayloads.subList(Math.max(0, a.allPayloads.size() - 3), a.allPayloads.size())
                            + " | B: " + b.last)
                    .isTrue();
            assertThat(a.last.lastRound().number()).isEqualTo(round);
            if (round == 1) {
                a.send("start", null);
                a.last = null;
                b.last = null;
            }
        }

        assertNoForeignCards(a, b);
        assertNoForeignCards(b, a);

        ResponseEntity<List<PlayerStats>> stats = rest.exchange("/api/rooms/" + code + "/stats", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {
                });
        assertThat(stats.getBody()).hasSize(7);
        assertThat(stats.getBody().stream().mapToLong(PlayerStats::kings).sum()).isEqualTo(2);
        assertThat(stats.getBody().stream().mapToLong(PlayerStats::rounds).sum()).isEqualTo(14);

        ResponseEntity<RoomView> me = rest.exchange("/api/rooms/" + code + "/me", HttpMethod.GET,
                new HttpEntity<>(tokenHeader(bence.token())), RoomView.class);
        assertThat(me.getBody().me()).isEqualTo(bence.playerId());

        a.session.disconnect();
        b.session.disconnect();
    }

    /**
     * Minden, a kliensnek küldött üzenetben csak olyan lapazonosító szerepelhet, ami a saját kezében,
     * a kupac tetején vagy a saját lapcseréjében van. A másik ember kezéből semmi.
     */
    private void assertNoForeignCards(Client mine, Client other) throws Exception {
        Set<String> otherHands = new HashSet<>();
        for (String payload : other.allPayloads) {
            JsonNode node = json.readTree(payload);
            if ("state".equals(node.path("type").asText())) {
                node.path("state").path("hand").forEach(h -> otherHands.add(h.asText()));
            }
        }
        assertThat(otherHands).isNotEmpty();
        int checked = 0;
        for (String payload : mine.allPayloads) {
            JsonNode node = json.readTree(payload);
            if (!"state".equals(node.path("type").asText())) {
                continue;
            }
            JsonNode state = node.path("state");
            Set<String> allowed = new HashSet<>();
            state.path("hand").forEach(h -> allowed.add(h.asText()));
            state.path("pile").path("topCards").forEach(h -> allowed.add(h.asText()));
            state.path("exchange").forEach(t -> t.path("cards").forEach(h -> allowed.add(h.asText())));
            Matcher m = CARD_ID.matcher(payload);
            while (m.find()) {
                assertThat(allowed).as("idegen lap az üzenetben: " + m.group()).contains(m.group());
                checked++;
            }
            for (JsonNode p : state.path("players")) {
                assertThat(p.has("hand")).isFalse();
            }
        }
        assertThat(checked).isGreaterThan(0);
    }
}
