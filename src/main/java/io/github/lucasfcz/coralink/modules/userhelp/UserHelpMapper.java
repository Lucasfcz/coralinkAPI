package io.github.lucasfcz.coralink.modules.userhelp;

import io.github.lucasfcz.coralink.modules.userhelp.dto.UserHelpResponse;
import io.github.lucasfcz.coralink.modules.userhelp.model.UserHelp;
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
