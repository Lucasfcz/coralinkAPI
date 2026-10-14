package io.github.lucasfcz.coralink.modules.usersuggestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_suggestions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSuggestion {

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

    // E-mail opcional do usuário, caso ele deseje receber retorno da equipe sobre a sugestão enviada.
    @Column
    private String userEmail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UserSuggestion(SuggestionType type, String suggestion, String userEmail) {
        this.type = type;
        this.suggestion = suggestion;
        this.userEmail = userEmail;
    }
}
