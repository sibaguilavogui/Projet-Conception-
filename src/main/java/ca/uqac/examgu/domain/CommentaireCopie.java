package ca.uqac.examgu.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Petit message de commentaire lié à une tentative d'examen.
 * Utilisé comme "mini chat" entre enseignant et étudiant.
 */
public class CommentaireCopie {

    private final UUID id;
    private final UUID tentativeId;
    private final LocalDateTime date;
    private final String auteurCode;
    private final Role auteurRole;
    private final String message;

    public CommentaireCopie(UUID tentativeId, String auteurCode, Role auteurRole, String message) {
        this.id = UUID.randomUUID();
        this.tentativeId = tentativeId;
        this.date = LocalDateTime.now();
        this.auteurCode = auteurCode;
        this.auteurRole = auteurRole;
        this.message = message;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTentativeId() {
        return tentativeId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getAuteurCode() {
        return auteurCode;
    }

    public Role getAuteurRole() {
        return auteurRole;
    }

    public String getMessage() {
        return message;
    }
}
