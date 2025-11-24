package domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Tentative {

    private final UUID id;
    private LocalDateTime debut = LocalDateTime.now();
    private LocalDateTime fin;
    private StatutTentative statut;
    private double score;
    private Examen examen;
    private Etudiant etudiant;
    private final List<ReponseDonnee> reponses;

    public Tentative(Examen examen, Etudiant etudiant) {
        this.id = UUID.randomUUID();
        this.debut = LocalDateTime.now();
        this.fin = examen.getDateFin();
        this.examen = examen;
        this.etudiant = etudiant;
        this.reponses = new ArrayList<>();
        this.score = 0;
        this.statut = StatutTentative.EN_COURS;
    }

    public void demarrer() {
        this.debut = LocalDateTime.now();
    }

    public int tempsRestant() {
        if (fin == null) return 0;
        return (int) java.time.Duration.between(LocalDateTime.now(), fin).getSeconds();
    }

    public void sauvegarderReponse(UUID questionId, String contenu, LocalDateTime now) {
        ReponseDonnee rep = reponses.stream()
                .filter(r -> r.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElse(null);

        if (rep == null) {
            Question q = examen.getQuestion(questionId);
            rep = new ReponseDonnee(q);
            this.addReponse(rep);
        }
        rep.mettreAJourContenu(contenu);
    }

    public void soumettre() {
        this.fin = LocalDateTime.now();
        this.statut = StatutTentative.SOUMISE;
        this.score = calculerScoreAuto();
    }

    public boolean estExpiree() {
        return fin != null && LocalDateTime.now().isAfter(fin);
    }

    public double calculerScoreAuto() {
        return reponses.stream()
                .mapToDouble(ReponseDonnee::getNotePartielle)
                .sum();
    }

    public void appliquerNoteManuelle(UUID questionId, double note) {
        reponses.stream()
                .filter(r -> r.getQuestion().getId().equals(questionId))
                .findFirst()
                .ifPresent(r -> r.noterPatiellement(note));
    }

    public UUID getId() {
        return id;
    }
    public LocalDateTime getFin() {
        return fin;
    }
    public void setFin(LocalDateTime fin) {
        this.fin = fin;
    }
    public LocalDateTime getDebut() {
        return debut;
    }
    public void setDebut(LocalDateTime debut) {
        this.debut = debut;
    }
    public StatutTentative getStatut() {
        return statut;
    }
    public void setStatut(StatutTentative statut) {
        this.statut = statut;
    }
    public double getScore() {
        return score;
    }
    public void setScore(double score) {
        this.score = score;
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
    public List<ReponseDonnee> getReponses() {
        return reponses;
    }
    public void addReponse(ReponseDonnee reponse) {
        this.reponses.add(reponse);
    }
}
