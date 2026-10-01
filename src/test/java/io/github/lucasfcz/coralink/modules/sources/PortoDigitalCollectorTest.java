package io.github.lucasfcz.coralink.modules.sources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class PortoDigitalCollectorTest extends BaseCollectorLiveTest {

    @Test
    @DisplayName("Teste de integração ao vivo: PORTO_DIGITAL")
    void testLiveCollection() {
        String token = resolveToken();
        PortoDigitalCollector collector = new PortoDigitalCollector(token);
        assertCollectorLive(collector, "PORTO_DIGITAL");
    }

    private String resolveToken() {
        String token = System.getenv("PORTO_DIGITAL_STORYBLOK_TOKEN");
        if (token != null && !token.isBlank()) return token;
        token = System.getProperty("PORTO_DIGITAL_STORYBLOK_TOKEN");
        if (token != null && !token.isBlank()) return token;
        try {
            Path envPath = Path.of(".env");
            if (!Files.exists(envPath)) {
                envPath = Path.of("../.env");
            }
            if (Files.exists(envPath)) {
                for (String line : Files.readAllLines(envPath)) {
                    if (line.startsWith("PORTO_DIGITAL_STORYBLOK_TOKEN=")) {
                        return line.substring("PORTO_DIGITAL_STORYBLOK_TOKEN=".length()).trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
