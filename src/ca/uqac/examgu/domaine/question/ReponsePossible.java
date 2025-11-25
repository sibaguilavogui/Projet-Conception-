package ca.uqac.examgu.domaine.question;

import java.util.Objects;
import java.util.UUID;

/** Réponse candidate pour une question (QCM / Vrai-Faux). */
public class ReponsePossible {

    private final UUID id;
    private String libelle;
    private boolean correcte;

    public ReponsePossible(UUID id, String libelle, boolean correcte) {
        if (id == null) throw new IllegalArgumentException("id requis");
        if (libelle == null || libelle.isBlank()) throw new IllegalArgumentException("libellé requis");
        this.id = id;
        this.libelle = libelle;
        this.correcte = correcte;
    }

    public void marquerCorrecte()   { this.correcte = true; }
    public void marquerIncorrecte() { this.correcte = false; }

    public UUID getId() { return id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) {
        if (libelle == null || libelle.isBlank()) throw new IllegalArgumentException("libellé requis");
        this.libelle = libelle;
    }

    public boolean isCorrecte() { return correcte; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReponsePossible)) return false;
        ReponsePossible that = (ReponsePossible) o;
        return id.equals(that.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        return "ReponsePossible{" + "id=" + id + ", correcte=" + correcte + '}';
    }
}
