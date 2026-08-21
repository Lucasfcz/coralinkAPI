package io.github.lucasfcz.coralink.mappers;

import io.github.lucasfcz.coralink.dto.UserHelpResponse;
import io.github.lucasfcz.coralink.model.UserHelp;
import org.springframework.stereotype.Component;

@Component
public class UserHelpMapper {

    public UserHelpResponse toResponse(UserHelp userHelp) {
        return new UserHelpResponse(
                userHelp.getId(),
                userHelp.getType(),
                userHelp.getSuggestion(),
                userHelp.getUserEmail()
        );
    }
}
