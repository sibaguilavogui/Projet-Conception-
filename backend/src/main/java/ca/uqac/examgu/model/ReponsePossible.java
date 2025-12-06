package ca.uqac.examgu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.persistence.GenerationType;

import java.util.UUID;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReponsePossible {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    private String libelle;
    private boolean correcte;

    protected ReponsePossible() {}

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

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }
}
