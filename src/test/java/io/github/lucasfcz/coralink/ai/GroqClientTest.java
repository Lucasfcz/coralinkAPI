package io.github.lucasfcz.coralink.ai;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
class GroqClientTest {

    @Autowired
    private GroqClient groqClient;

    @Test
    void testSendPromptBasicValidation() {
        String systemPrompt = "You are a helpful assistant that responds only in valid JSON format. "
                + "Always respond with a JSON object containing a 'message' key.";
        String userPrompt = "Say hello, respond with a JSON object containing a key 'message' with your greeting.";

        String result = groqClient.sendPrompt(systemPrompt, userPrompt);

        System.out.println("=== GroqClient Test Result ===");
        System.out.println("Response: " + result);
        System.out.println("==============================");

        log.info("GroqClient response: {}", result);

        assertNotNull(result, "Response should not be null");
        assertTrue(!result.isEmpty(), "Response should not be empty");
        assertTrue(result.contains("message"), "Response should contain 'message' key");
    }
}
