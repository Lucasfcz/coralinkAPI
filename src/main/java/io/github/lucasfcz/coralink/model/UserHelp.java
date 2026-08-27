package io.github.lucasfcz.coralink.model;

import io.github.lucasfcz.coralink.enums.SuggestionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_help")
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

    @Column(columnDefinition = "TEXT", nullable = false)
    @Size(max = 5000)
    private String suggestion;

    // E-mail opcional do usuário, caso ele deseje receber retorno da equipe sobre a sugestão ou ajuda enviada.
    @Column
    private String userEmail;


    @Builder

    public UserHelp(SuggestionType type, String suggestion, String userEmail) {
        this.type = type;
        this.suggestion = suggestion;
        this.userEmail = userEmail;
    }
}
