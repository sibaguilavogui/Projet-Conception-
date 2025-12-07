package ca.uqac.examgu.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class CommentaireCopie {

    private final UUID id;
    private final UUID tentativeId;
    private final LocalDateTime date;
    private final String auteurCode;
    private final Role auteurRole;
    private final String message;

    public CommentaireCopie(UUID id,
                            UUID tentativeId,
                            LocalDateTime date,
                            String auteurCode,
                            Role auteurRole,
                            String message) {
        this.id = id;
        this.tentativeId = tentativeId;
        this.date = date;
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
