package ca.uqac.examgu.model;

import jakarta.persistence.*;
import jakarta.persistence.GenerationType;

import java.util.UUID;

@Entity
public class ReponsePossible {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    private final String libelle;
    private boolean correcte;

    public ReponsePossible(String libelle, boolean correcte) {
        this.libelle = libelle;
        this.correcte = correcte;
    }

    public UUID getId() { return id; }
    public String getLibelle() { return libelle; }
    public boolean isCorrecte() { return correcte; }

    public void marquerIncorrecte() {
        this.correcte=false;
    }

    public void marquerCorrecte() {
        this.correcte=true;
    }
}
