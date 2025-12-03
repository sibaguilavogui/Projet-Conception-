package ca.uqac.examgu.domain;

import java.util.UUID;

public class ReponsePossible {
    private final UUID id;
    private final String libelle;
    private final boolean correcte;

    public ReponsePossible(String libelle, boolean correcte) {
        this(UUID.randomUUID(), libelle, correcte);
    }

    public ReponsePossible(UUID id, String libelle, boolean correcte) {
        this.id = (id != null) ? id : UUID.randomUUID();
        this.libelle = libelle;
        this.correcte = correcte;
    }

    public UUID getId() { return id; }
    public String getLibelle() { return libelle; }
    public boolean isCorrecte() { return correcte; }
}
