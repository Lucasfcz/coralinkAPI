package io.github.lucasfcz.coralink.model;

import io.github.lucasfcz.coralink.enums.SuggestionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserHelp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SuggestionType type;

    @Column(nullable = false)
    @Max(5000)
    private String suggestion;

    @Column
    private String userEmail; // Is optional for send a response by email if the user want

    @Builder

    public UserHelp(SuggestionType type, String suggestion, String userEmail) {
        this.type = type;
        this.suggestion = suggestion;
        this.userEmail = userEmail;
    }
}
