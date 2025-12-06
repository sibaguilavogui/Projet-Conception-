package ca.uqac.examgu.dto;

import ca.uqac.examgu.model.QuestionADeveloppement;

import java.util.UUID;

public class QuestionADeveloppementDTO {
    private UUID id;
    private String enonce;
    private double bareme;
    private UUID examenId;

    // Constructeurs
    public QuestionADeveloppementDTO() {}

    public QuestionADeveloppementDTO(UUID id, String enonce, double bareme,
                                     UUID examenId, boolean estAutoCorrectible) {
        this.id = id;
        this.enonce = enonce;
        this.bareme = bareme;
        this.examenId = examenId;
    }

    // Getters et Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEnonce() { return enonce; }
    public void setEnonce(String enonce) { this.enonce = enonce; }

    public double getBareme() { return bareme; }
    public void setBareme(double bareme) { this.bareme = bareme; }

    public UUID getExamenId() { return examenId; }
    public void setExamenId(UUID examenId) { this.examenId = examenId; }

}