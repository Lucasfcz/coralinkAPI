package io.github.lucasfcz.coralink.modules.ai;

import io.github.lucasfcz.coralink.modules.ai.dto.ScreeningBatchResult;
import io.github.lucasfcz.coralink.modules.ai.dto.ScreeningResult;
import io.github.lucasfcz.coralink.modules.opportunity.enums.OpportunityType;
import io.github.lucasfcz.coralink.infra.exception.AiCallException;
import io.github.lucasfcz.coralink.modules.pipeline.model.RawOpportunity;
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
                new ScreeningBatchResult(List.of(new ScreeningResult(42L, true))));
        ScreeningService service = new ScreeningService(ai);

        ScreeningBatchResult result = service.screen(List.of(opportunity));

        assertEquals(42L, result.screeningResults().getFirst().rawOpportunityId());
    }

    private RawOpportunity rawOpportunity(Long id) throws Exception {
        RawOpportunity opportunity = new RawOpportunity(
                "Workshop Java",
                "Uma oportunidade",
                "https://portal.cin.ufpe.br/news/workshop",
                "CIN_UFPE",
                null,
                false
        );
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
