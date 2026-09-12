package io.github.lucasfcz.coralink.infra.config;

import io.github.lucasfcz.coralink.modules.opportunity.dto.OpportunityResponse;
import io.github.lucasfcz.coralink.modules.opportunity.enums.Modality;
import io.github.lucasfcz.coralink.modules.opportunity.enums.OpportunityType;
import io.github.lucasfcz.coralink.modules.opportunity.enums.TargetCourseAudience;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RedisCacheSerializationTest {

    private final RedisSerializer<Object> serializer = RedisCacheConfig.createJsonSerializer();

    private OpportunityResponse createSampleResponse(Long id) {
        return new OpportunityResponse(
                id,
                "Workshop de Inteligência Artificial",
                "Descrição completa do evento",
                OpportunityType.WORKSHOP,
                "Inteligência Artificial",
                Set.of(TargetCourseAudience.COMPUTER_SCIENCE, TargetCourseAudience.ADS),
                Modality.ONLINE,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 9, 28),
                "Online",
                "https://example.com/ai-workshop",
                "CIN_UFPE",
                "https://example.com/banner.png",
                true,
                true,
                LocalDate.of(2026, 10, 1)
        );
    }

    @Test
    @DisplayName("Should serialize and deserialize OpportunityResponse correctly with LocalDate")
    void shouldSerializeAndDeserializeOpportunityResponse() {
        OpportunityResponse original = createSampleResponse(100L);

        byte[] serialized = serializer.serialize(original);
        assertNotNull(serialized);
        assertTrue(serialized.length > 0);

        Object deserialized = serializer.deserialize(serialized);
        assertNotNull(deserialized);
        assertInstanceOf(OpportunityResponse.class, deserialized);

        OpportunityResponse result = (OpportunityResponse) deserialized;
        assertEquals(100L, result.id());
        assertEquals("Workshop de Inteligência Artificial", result.title());
        assertEquals(LocalDate.of(2026, 10, 1), result.expiresAt());
        assertEquals(LocalDate.of(2026, 9, 28), result.registrationDeadline());
        assertEquals("CIN_UFPE", result.sourceName());
        assertEquals(OpportunityType.WORKSHOP, result.type());
    }

    @Test
    @DisplayName("Should serialize and deserialize PageImpl with OpportunityResponse without errors")
    @SuppressWarnings("unchecked")
    void shouldSerializeAndDeserializePageImpl() {
        OpportunityResponse item1 = createSampleResponse(1L);
        OpportunityResponse item2 = createSampleResponse(2L);
        Page<OpportunityResponse> originalPage = new PageImpl<>(
                List.of(item1, item2),
                PageRequest.of(0, 10),
                2
        );

        byte[] serialized = serializer.serialize(originalPage);
        assertNotNull(serialized);
        assertTrue(serialized.length > 0);

        Object deserialized = serializer.deserialize(serialized);
        assertNotNull(deserialized);
        assertInstanceOf(Page.class, deserialized);

        Page<?> resultPage = (Page<?>) deserialized;
        assertEquals(2, resultPage.getTotalElements());
        assertEquals(2, resultPage.getContent().size());
        assertEquals(0, resultPage.getNumber());
    }
}
