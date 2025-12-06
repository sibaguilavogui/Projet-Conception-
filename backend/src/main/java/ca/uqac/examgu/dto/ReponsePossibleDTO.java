// ReponsePossibleDTO.java
package ca.uqac.examgu.dto;

import ca.uqac.examgu.model.ReponsePossible;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class ReponsePossibleDTO {
    private UUID id;
    @JsonProperty("texte")
    private String libelle;
    @JsonProperty("estCorrecte")
    private boolean correcte;

    // Constructeurs
    public ReponsePossibleDTO() {}

    public ReponsePossibleDTO(UUID id, String libelle, boolean correcte) {
        this.id = id;
        this.libelle = libelle;
        this.correcte = correcte;
    }

    // Méthode de conversion depuis l'entité
    public static ReponsePossibleDTO fromEntity(ReponsePossible reponse) {
        return new ReponsePossibleDTO(
                reponse.getId(),
                reponse.getLibelle(),
                reponse.isCorrecte()
        );
    }

    // Getters et Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getLibelle() { return libelle; }
    @JsonProperty("texte")
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public boolean isCorrecte() { return correcte; }
    @JsonProperty("estCorrecte")
    public void setCorrecte(boolean correcte) { this.correcte = correcte; }
}