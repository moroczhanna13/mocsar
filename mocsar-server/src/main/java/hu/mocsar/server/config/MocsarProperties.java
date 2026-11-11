package hu.mocsar.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param botDelayMs   ennyit vár a bot két lépés között (0 a tesztekben)
 * @param roomCodeLength a szobakód hossza
 */
@ConfigurationProperties(prefix = "mocsar")
public record MocsarProperties(long botDelayMs, int roomCodeLength) {

    public MocsarProperties {
        if (roomCodeLength <= 0) {
            roomCodeLength = 5;
        }
    }
}
