package io.github.lucasfcz.coralink.controller;

import io.github.lucasfcz.coralink.dto.UserHelpRequest;
import io.github.lucasfcz.coralink.dto.UserHelpResponse;
import io.github.lucasfcz.coralink.enums.SuggestionType;
import io.github.lucasfcz.coralink.repositories.UserHelpRepository;
import io.github.lucasfcz.coralink.services.UserHelpService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/suggestion")
@RequiredArgsConstructor
public class UserHelpController {

    private final UserHelpService userHelpService;

    public ResponseEntity<UserHelpResponse> createUserHelp(@Valid @RequestBody UserHelpRequest request) {
        return  ResponseEntity.ok(userHelpService.createUserHelp(request));
    }

    public ResponseEntity<Page<UserHelpResponse>> getAllUserHelp(Pageable pageable) {
        return ResponseEntity.ok(userHelpService.findAllUserHelp(pageable));
    }

    public ResponseEntity<Page<UserHelpResponse>> getAllUserHelpByType(@RequestParam SuggestionType type, Pageable pageable) {
        return ResponseEntity.ok(userHelpService.getUserHelpsByType(type, pageable));
    }
}
