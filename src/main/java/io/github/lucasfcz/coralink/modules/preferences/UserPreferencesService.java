package io.github.lucasfcz.coralink.modules.preferences;

import io.github.lucasfcz.coralink.infra.exception.NotFoundException;
import io.github.lucasfcz.coralink.modules.preferences.dto.UserPreferencesPatchRequest;
import io.github.lucasfcz.coralink.modules.preferences.dto.UserPreferencesRequest;
import io.github.lucasfcz.coralink.modules.preferences.dto.UserPreferencesResponse;
import io.github.lucasfcz.coralink.modules.preferences.model.UserPreferences;
import io.github.lucasfcz.coralink.modules.preferences.repository.UserPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    private final UserPreferencesRepository preferencesRepository;

    @Transactional(readOnly = true)
    public UserPreferencesResponse getPreferencesByUserId(Long userId) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Preferências não encontradas para o usuário id: " + userId));
        return toResponse(prefs);
    }

    @Transactional(readOnly = true)
    public UserPreferences getPreferencesEntityByUserId(Long userId) {
        return preferencesRepository.findByUserId(userId).orElse(null);
    }

    @Transactional
    @CacheEvict(value = "opportunities", allEntries = true)
    public UserPreferencesResponse savePreferences(Long userId, UserPreferencesRequest request) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseGet(() -> UserPreferences.builder().userId(userId).build());

        prefs.setSelectedTypes(request.selectedTypes() != null ? new HashSet<>(request.selectedTypes()) : new HashSet<>());
        prefs.setInstitutions(request.institutions() != null ? new HashSet<>(request.institutions()) : new HashSet<>());
        prefs.setTargetCourses(request.targetCourses() != null ? new HashSet<>(request.targetCourses()) : new HashSet<>());
        prefs.setNotInCollege(Boolean.TRUE.equals(request.notInCollege()));

        UserPreferences saved = preferencesRepository.save(prefs);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "opportunities", allEntries = true)
    public UserPreferencesResponse updatePreferences(Long userId, UserPreferencesPatchRequest request) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseGet(() -> UserPreferences.builder().userId(userId).build());

        if (request.selectedTypes() != null) {
            prefs.setSelectedTypes(new HashSet<>(request.selectedTypes()));
        }
        if (request.institutions() != null) {
            prefs.setInstitutions(new HashSet<>(request.institutions()));
        }
        if (request.targetCourses() != null) {
            prefs.setTargetCourses(new HashSet<>(request.targetCourses()));
        }
        if (request.notInCollege() != null) {
            prefs.setNotInCollege(request.notInCollege());
        }

        UserPreferences saved = preferencesRepository.save(prefs);
        return toResponse(saved);
    }

    private UserPreferencesResponse toResponse(UserPreferences entity) {
        return new UserPreferencesResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getSelectedTypes() != null ? new HashSet<>(entity.getSelectedTypes()) : Set.of(),
                entity.getInstitutions() != null ? new HashSet<>(entity.getInstitutions()) : Set.of(),
                entity.getTargetCourses() != null ? new HashSet<>(entity.getTargetCourses()) : Set.of(),
                entity.isNotInCollege(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
