package io.github.lucasfcz.coralink.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class GroqClient {

    private final ChatClient chatClient;

    public GroqClient(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String sendPrompt(String systemPrompt, String userPrompt) {

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    public <T> T sendStructuredPrompt(String systemPrompt, String userPrompt, Class<T> responseClassType) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(responseClassType);
    }
}