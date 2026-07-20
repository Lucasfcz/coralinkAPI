package io.github.lucasfcz.coralink.ai;

import io.github.lucasfcz.coralink.exceptions.AiCallException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GroqClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GroqClient(
            ObjectMapper objectMapper,
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.model}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.groq.com/openai")
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String sendPrompt(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            String json = restClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

            var response = objectMapper.readTree(json);

            return response
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage());
            throw new AiCallException("Failed to call Groq API", e.getCause());
        }
    }
}