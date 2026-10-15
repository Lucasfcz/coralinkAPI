package io.github.lucasfcz.coralink.modules.usersuggestion;

import io.github.lucasfcz.coralink.modules.usersuggestion.dto.UserSuggestionRequest;
import io.github.lucasfcz.coralink.modules.usersuggestion.dto.UserSuggestionResponse;
import io.github.lucasfcz.coralink.modules.usersuggestion.model.SuggestionType;
import io.github.lucasfcz.coralink.modules.usersuggestion.model.UserSuggestion;
import io.github.lucasfcz.coralink.modules.usersuggestion.repository.UserSuggestionRepository;
import io.github.lucasfcz.coralink.modules.auth.model.User;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserSuggestionService {

    private final UserSuggestionRepository userSuggestionRepository;
    private final UserSuggestionMapper userSuggestionMapper;

    public UserSuggestionResponse createUserSuggestion(UserSuggestionRequest request, User currentUser) {
        String effectiveEmail = (request.userEmail() != null && !request.userEmail().isBlank())
                ? request.userEmail().trim()
                : (currentUser != null ? currentUser.getEmail() : null);

        UserSuggestion userSuggestion = new UserSuggestion(
                request.type(),
                request.suggestion(),
                effectiveEmail
        );

        return userSuggestionMapper.toResponse(userSuggestionRepository.save(userSuggestion));
    }

    public UserSuggestionResponse createUserSuggestion(UserSuggestionRequest request) {
        return createUserSuggestion(request, null);
    }

    public Page<UserSuggestionResponse> getUserSuggestionsByType(SuggestionType type, Pageable pageable) {
        return userSuggestionRepository.findAllByType(type, pageable).map(userSuggestionMapper::toResponse);
    }

    public Page<UserSuggestionResponse> findAllUserSuggestions(Pageable pageable) {
        return userSuggestionRepository.findAll(pageable).map(userSuggestionMapper::toResponse);
    }
}
