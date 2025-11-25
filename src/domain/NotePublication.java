package domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class NotePublication {

    private UUID id;
    private boolean publieAuxEtudiants;
    private LocalDateTime datePublication;

    public NotePublication() {
        this(UUID.randomUUID(), false, null);
    }

    public NotePublication(UUID id, boolean publieAuxEtudiants, LocalDateTime datePublication) {
        this.id = (id != null) ? id : UUID.randomUUID();
        this.publieAuxEtudiants = publieAuxEtudiants;
        this.datePublication = datePublication;
    }

    public void publier(LocalDateTime now) {
        Objects.requireNonNull(now, "now ne doit pas être null");
        this.publieAuxEtudiants = true;
        this.datePublication = now;
    }

    public void retirer() {
        this.publieAuxEtudiants = false;
    }

    public boolean estVisible() {
        return publieAuxEtudiants && datePublication != null;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = (id != null) ? id : UUID.randomUUID();
    }

    public boolean isPublieAuxEtudiants() {
        return publieAuxEtudiants;
    }

    public void setPublieAuxEtudiants(boolean publieAuxEtudiants) {
        this.publieAuxEtudiants = publieAuxEtudiants;
    }

    public LocalDateTime getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(LocalDateTime datePublication) {
        this.datePublication = datePublication;
    }
}
