package io.github.lucasfcz.coralink.modules.usersuggestion;

import io.github.lucasfcz.coralink.modules.usersuggestion.dto.UserSuggestionResponse;
import io.github.lucasfcz.coralink.modules.usersuggestion.model.UserSuggestion;
import org.springframework.stereotype.Component;

@Component
public class UserSuggestionMapper {

    public UserSuggestionResponse toResponse(UserSuggestion userSuggestion) {
        return new UserSuggestionResponse(
                userSuggestion.getId(),
                userSuggestion.getType(),
                userSuggestion.getSuggestion(),
                userSuggestion.getUserEmail(),
                userSuggestion.getCreatedAt()
        );
    }
}
