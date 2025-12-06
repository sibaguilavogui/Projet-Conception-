package ca.uqac.examgu.dto;

import ca.uqac.examgu.model.Enumerations.PolitiqueCorrectionQCM;
import ca.uqac.examgu.model.QuestionAChoix;
import ca.uqac.examgu.model.ReponsePossible;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class QuestionAChoixDTO {
    private UUID id;
    private String enonce;
    private double bareme;
    private UUID examenId;
    private QuestionAChoix.TypeChoix typeChoix;
    private int nombreChoixMin;
    private int nombreChoixMax;
    private PolitiqueCorrectionQCM politiqueCorrectionQCM;
    private List<ReponsePossibleDTO> options;

    public QuestionAChoixDTO() {}

    public QuestionAChoixDTO(UUID id, String enonce, double bareme, UUID examenId,
                             QuestionAChoix.TypeChoix typeChoix,
                             int nombreChoixMin, int nombreChoixMax, PolitiqueCorrectionQCM politiqueCorrectionQCM,
                             List<ReponsePossibleDTO> options) {
        this.id = id;
        this.enonce = enonce;
        this.bareme = bareme;
        this.examenId = examenId;
        this.typeChoix = typeChoix;
        this.nombreChoixMin = nombreChoixMin;
        this.nombreChoixMax = nombreChoixMax;
        this.politiqueCorrectionQCM = politiqueCorrectionQCM;
        this.options = options;
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

    public QuestionAChoix.TypeChoix getTypeChoix() { return typeChoix; }
    public void setTypeChoix(QuestionAChoix.TypeChoix typeChoix) { this.typeChoix = typeChoix; }

    public int getNombreChoixMin() { return nombreChoixMin; }
    public void setNombreChoixMin(int nombreChoixMin) { this.nombreChoixMin = nombreChoixMin; }

    public int getNombreChoixMax() { return nombreChoixMax; }
    public void setNombreChoixMax(int nombreChoixMax) { this.nombreChoixMax = nombreChoixMax; }

    public PolitiqueCorrectionQCM getPolitiqueCorrectionQCM() {
        return politiqueCorrectionQCM;
    }

    public void setPolitiqueCorrectionQCM(PolitiqueCorrectionQCM politiqueCorrectionQCM) {
        this.politiqueCorrectionQCM = politiqueCorrectionQCM;
    }

    public List<ReponsePossibleDTO> getOptions() {
        return options;
    }

    public void setOptions(List<ReponsePossibleDTO> options) {
        this.options = options;
    }
}