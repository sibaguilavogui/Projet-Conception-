package domain;

import java.time.LocalDateTime;

public class NotePublication {
    private boolean publiee = false;
    private LocalDateTime datePublication;

    public void publier(LocalDateTime now) {
        this.publiee = true;
        this.datePublication = now;
    }

    public boolean estPubliee() {
        return publiee;
    }

    public LocalDateTime getDatePublication() {
        return datePublication;
    }
}
