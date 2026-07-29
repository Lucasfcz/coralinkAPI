package io.github.lucasfcz.coralink.ai;

import io.github.lucasfcz.coralink.exceptions.AiCallException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class AiClient {

    private final ChatClient chatClient;

    public AiClient(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public <T> T sendPrompt(String systemPrompt, String userPrompt, Class<T> responseClassType) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(responseClassType);
        } catch (RuntimeException e) {
            throw new AiCallException("Failed to send prompt to AI", e);
        }
    }
}
