package io.github.lucasfcz.coralink.modules.preferences;

import io.github.lucasfcz.coralink.infra.exception.NotFoundException;
import io.github.lucasfcz.coralink.modules.opportunity.enums.OpportunityType;
import io.github.lucasfcz.coralink.modules.opportunity.enums.TargetCourseAudience;
import io.github.lucasfcz.coralink.modules.preferences.dto.UserPreferencesPatchRequest;
import io.github.lucasfcz.coralink.modules.preferences.dto.UserPreferencesRequest;
import io.github.lucasfcz.coralink.modules.preferences.dto.UserPreferencesResponse;
import io.github.lucasfcz.coralink.modules.preferences.model.UserPreferences;
import io.github.lucasfcz.coralink.modules.preferences.repository.UserPreferencesRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferencesServiceTest {

    @Mock
    private UserPreferencesRepository preferencesRepository;

    @InjectMocks
    private UserPreferencesService preferencesService;

    @Test
    @DisplayName("Deve retornar preferencias existentes do usuario com sucesso")
    void shouldReturnExistingPreferences() {
        Long userId = 42L;
        UserPreferences prefs = UserPreferences.builder()
                .id(1L)
                .userId(userId)
                .selectedTypes(Set.of(OpportunityType.WORKSHOP))
                .institutions(Set.of("UFPE"))
                .targetCourses(Set.of(TargetCourseAudience.ADS))
                .notInCollege(false)
                .build();

        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(prefs));

        UserPreferencesResponse response = preferencesService.getPreferencesByUserId(userId);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertTrue(response.selectedTypes().contains(OpportunityType.WORKSHOP));
        assertTrue(response.institutions().contains("UFPE"));
        assertTrue(response.targetCourses().contains(TargetCourseAudience.ADS));
        assertFalse(response.notInCollege());
    }

    @Test
    @DisplayName("Deve lancar NotFoundException se preferencias nao existirem")
    void shouldThrowNotFoundWhenPreferencesDoNotExist() {
        Long userId = 99L;
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> preferencesService.getPreferencesByUserId(userId));
    }

    @Test
    @DisplayName("Deve criar novas preferencias via savePreferences")
    void shouldCreateNewPreferencesOnSave() {
        Long userId = 10L;
        UserPreferencesRequest request = new UserPreferencesRequest(
                Set.of(OpportunityType.INTERNSHIP),
                Set.of("UPE"),
                Set.of(TargetCourseAudience.COMPUTER_SCIENCE),
                false
        );

        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferencesRepository.save(any(UserPreferences.class))).thenAnswer(invocation -> {
            UserPreferences entity = invocation.getArgument(0);
            entity.setId(100L);
            return entity;
        });

        UserPreferencesResponse response = preferencesService.savePreferences(userId, request);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertTrue(response.selectedTypes().contains(OpportunityType.INTERNSHIP));
        assertTrue(response.institutions().contains("UPE"));
        assertTrue(response.targetCourses().contains(TargetCourseAudience.COMPUTER_SCIENCE));
        assertFalse(response.notInCollege());
        verify(preferencesRepository, times(1)).save(any(UserPreferences.class));
    }

    @Test
    @DisplayName("Deve atualizar parcialmente campos via updatePreferences (PATCH)")
    void shouldUpdatePartiallyOnPatch() {
        Long userId = 20L;
        UserPreferences existing = UserPreferences.builder()
                .id(2L)
                .userId(userId)
                .selectedTypes(Set.of(OpportunityType.EVENT))
                .institutions(Set.of("UFPE"))
                .targetCourses(Set.of(TargetCourseAudience.DATA_SCIENCE))
                .notInCollege(false)
                .build();

        UserPreferencesPatchRequest patch = new UserPreferencesPatchRequest(
                null,
                Set.of("UNICAP"),
                null,
                true
        );

        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(preferencesRepository.save(any(UserPreferences.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferencesResponse response = preferencesService.updatePreferences(userId, patch);

        assertNotNull(response);
        // Tipos e cursos originais devem ser preservados
        assertTrue(response.selectedTypes().contains(OpportunityType.EVENT));
        assertTrue(response.targetCourses().contains(TargetCourseAudience.DATA_SCIENCE));
        // Instituicoes e notInCollege devem ser atualizados
        assertTrue(response.institutions().contains("UNICAP"));
        assertFalse(response.institutions().contains("UFPE"));
        assertTrue(response.notInCollege());
        verify(preferencesRepository, times(1)).save(existing);
    }
}
