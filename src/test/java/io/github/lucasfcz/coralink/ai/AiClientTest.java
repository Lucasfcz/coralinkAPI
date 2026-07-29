package io.github.lucasfcz.coralink.ai;

import io.github.lucasfcz.coralink.dto.ScreeningBatchResult;
import io.github.lucasfcz.coralink.dto.ScreeningResult;
import io.github.lucasfcz.coralink.enums.OpportunityType;
import io.github.lucasfcz.coralink.enums.SourceName;
import io.github.lucasfcz.coralink.exceptions.AiCallException;
import io.github.lucasfcz.coralink.model.RawOpportunity;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AiClientTest {

    @Test
    void validatesThatTheAiReturnsExactlyOneResultForEveryOpportunity() throws Exception {
        RawOpportunity opportunity = rawOpportunity(42L);
        AiClient ai = fixedResponse(
                new ScreeningBatchResult(List.of(new ScreeningResult(42L, true, OpportunityType.WORKSHOP, "É um workshop."))));
        ScreeningService service = new ScreeningService(ai);

        ScreeningBatchResult result = service.screen(List.of(opportunity));

        assertEquals(42L, result.screeningResults().getFirst().rawOpportunityId());
    }

    @Test
    void rejectsAnIncompleteAiResponse() throws Exception {
        AiClient ai = fixedResponse(new ScreeningBatchResult(List.of()));
        ScreeningService service = new ScreeningService(ai);

        assertThrows(AiCallException.class, () -> service.screen(List.of(rawOpportunity(42L))));
    }

    private RawOpportunity rawOpportunity(Long id) throws Exception {
        RawOpportunity opportunity = RawOpportunity.builder()
                .title("Workshop Java")
                .shortSummary("Uma oportunidade")
                .newsUrl("https://portal.cin.ufpe.br/news/workshop")
                .sourceName(SourceName.CIN_UFPE)
                .becameOpportunity(false)
                .build();
        Field field = RawOpportunity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(opportunity, id);
        return opportunity;
    }

    private AiClient fixedResponse(ScreeningBatchResult response) {
        ChatClient.Builder builderMock = mock(ChatClient.Builder.class);

        return new AiClient(builderMock) {
            @Override
            public <T> T sendPrompt(String systemPrompt, String userPrompt, Class<T> responseClassType) {
                return responseClassType.cast(response);
            }
        };
    }
}
