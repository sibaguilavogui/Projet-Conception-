// ReponsePossibleDTO.java
package ca.uqac.examgu.dto;

import ca.uqac.examgu.model.ReponsePossible;

import java.util.UUID;

public class ReponsePossibleDTO {
    private UUID id;
    private String libelle;
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
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public boolean isCorrecte() { return correcte; }
    public void setCorrecte(boolean correcte) { this.correcte = correcte; }
}