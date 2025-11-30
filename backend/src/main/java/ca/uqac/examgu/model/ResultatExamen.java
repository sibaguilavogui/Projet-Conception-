package ca.uqac.examgu.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "resultats_examen")
public class ResultatExamen {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "examen_id", nullable = false)
    private Examen examen;

    @ManyToOne
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @OneToOne
    @JoinColumn(name = "tentative_id")
    private Tentative tentative;

    private double score;
    private double scoreMax;
    private boolean visible;
    private boolean correctionAutoComplete;

    public ResultatExamen() {
    }

    public ResultatExamen(Tentative tentative) {
        this.tentative = tentative;
        this.examen = tentative.getExamen();
        this.etudiant = tentative.getEtudiant();
        this.score = tentative.getScore();
        this.scoreMax = tentative.getExamen().totalPoints();
        this.visible = tentative.getExamen().getPublication() != null &&
                tentative.getExamen().getPublication().isEstPubliee();
        this.correctionAutoComplete = estCorrectionAutoComplete(tentative);
    }

    public ResultatExamen(Examen examen, Etudiant etudiant, double score) {
        this.examen = examen;
        this.etudiant = etudiant;
        this.score = score;
        this.scoreMax = examen.totalPoints();
        this.visible = examen.getPublication() != null &&
                examen.getPublication().isEstPubliee();
        this.correctionAutoComplete = false;
    }

    private boolean estCorrectionAutoComplete(Tentative tentative) {
        return tentative.getExamen().getQuestions().stream()
                .allMatch(question -> {
                    if (question instanceof QuestionAChoix) {
                        return tentative.getReponses().stream()
                                .anyMatch(reponse ->
                                        reponse.getQuestion().getId().equals(question.getId()) &&
                                                reponse.getNotePartielle() >= 0);
                    }
                    return true;
                });
    }

    public double getPourcentage() {
        return scoreMax > 0 ? (score / scoreMax) * 100 : 0;
    }

    public String getStatut() {
        if (!visible) return "NON_PUBLIE";
        if (score >= scoreMax * 0.7) return "EXCELLENT";
        if (score >= scoreMax * 0.5) return "SATISFAISANT";
        return "INSUFFISANT";
    }

    public boolean estAdmis() {
        return score >= scoreMax * 0.5;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Examen getExamen() {
        return examen;
    }

    public void setExamen(Examen examen) {
        this.examen = examen;
    }

    public Etudiant getEtudiant() {
        return etudiant;
    }

    public void setEtudiant(Etudiant etudiant) {
        this.etudiant = etudiant;
    }

    public Tentative getTentative() {
        return tentative;
    }

    public void setTentative(Tentative tentative) {
        this.tentative = tentative;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getScoreMax() {
        return scoreMax;
    }

    public void setScoreMax(double scoreMax) {
        this.scoreMax = scoreMax;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isCorrectionAutoComplete() {
        return correctionAutoComplete;
    }

    public void setCorrectionAutoComplete(boolean correctionAutoComplete) {
        this.correctionAutoComplete = correctionAutoComplete;
    }


    @Override
    public String toString() {
        return String.format("ResultatExamen{examen=%s, etudiant=%s, score=%.2f/%.2f, visible=%s}",
                examen.getTitre(), etudiant.getNom(), score, scoreMax, visible);
    }
}