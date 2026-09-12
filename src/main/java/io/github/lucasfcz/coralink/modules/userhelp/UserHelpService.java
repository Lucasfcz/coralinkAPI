package io.github.lucasfcz.coralink.modules.userhelp;

import io.github.lucasfcz.coralink.modules.userhelp.dto.UserHelpRequest;
import io.github.lucasfcz.coralink.modules.userhelp.dto.UserHelpResponse;
import io.github.lucasfcz.coralink.modules.userhelp.model.SuggestionType;
import io.github.lucasfcz.coralink.modules.userhelp.UserHelpMapper;
import io.github.lucasfcz.coralink.modules.userhelp.model.UserHelp;
import io.github.lucasfcz.coralink.modules.userhelp.repository.UserHelpRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserHelpService {

    private final UserHelpRepository userHelpRepository;
    private final UserHelpMapper userHelpMapper;

    public UserHelpResponse createUserHelp(UserHelpRequest request) {
        UserHelp userHelp = new UserHelp(
                request.type(),
                request.suggestion(),
                request.userEmail()
        );

        return userHelpMapper.toResponse(userHelpRepository.save(userHelp));
    }

    public Page<UserHelpResponse> getUserHelpsByType(SuggestionType type, Pageable pageable) {
        return userHelpRepository.findAllByType(type, pageable).map(userHelpMapper::toResponse);
    }

    public Page<UserHelpResponse> findAllUserHelp(Pageable pageable) {
        return userHelpRepository.findAll(pageable).map(userHelpMapper::toResponse);
    }
}
