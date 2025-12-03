package ca.uqac.examgu.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class Tentative {

    private UUID id;
    private LocalDateTime debut;
    private LocalDateTime fin;
    private StatutTentative statut;
    private double score; // score total (auto + manuel)

    private Examen examen;
    private Etudiant etudiant;
    private final List<ReponseDonnee> reponses;

    // ✅ Notes/commentaires manuels par questionId (surtout pour COURTE)
    private final Map<UUID, Double> notesManuelles = new HashMap<>();
    private final Map<UUID, String> commentairesManuels = new HashMap<>();

    public Tentative(UUID id,
                     LocalDateTime debut,
                     LocalDateTime fin,
                     StatutTentative statut,
                     double score,
                     Examen examen,
                     Etudiant etudiant) {

        this.id = (id != null) ? id : UUID.randomUUID();
        this.debut = debut;
        this.fin = fin;
        this.statut = (statut != null) ? statut : StatutTentative.EN_COURS;
        this.score = score;
        this.examen = Objects.requireNonNull(examen, "examen ne doit pas être null");
        this.etudiant = Objects.requireNonNull(etudiant, "etudiant ne doit pas être null");
        this.reponses = new ArrayList<>();
    }

    public Tentative(Examen examen, Etudiant etudiant) {
        this(UUID.randomUUID(), null, null, StatutTentative.EN_COURS, 0.0, examen, etudiant);
    }

    public void demarrer(LocalDateTime now) {
        Objects.requireNonNull(now, "now ne doit pas être null");
        if (debut != null) return; // déjà démarrée
        this.debut = now;
        this.statut = StatutTentative.EN_COURS;
    }

    public int tempsRestant(LocalDateTime now) {
        Objects.requireNonNull(now, "now ne doit pas être null");
        LocalDateTime deadline = calculerDeadline();
        if (deadline == null) return 0;
        long mins = Duration.between(now, deadline).toMinutes();
        return (int) Math.max(0, mins);
    }

    public void sauvegarderReponse(UUID questionId, String contenu, LocalDateTime now) {
        Objects.requireNonNull(now, "now ne doit pas être null");
        if (!peutModifier(now)) return;

        Question q = examen.trouverQuestion(questionId);
        if (q == null) return;

        ReponseDonnee rd = trouverReponse(questionId);
        if (rd == null) {
            rd = new ReponseDonnee(q);
            reponses.add(rd);
        }
        rd.mettreAJourContenu(contenu, now);
    }

    public void soumettre(LocalDateTime now) {
        Objects.requireNonNull(now, "now ne doit pas être null");
        if (statut != StatutTentative.EN_COURS) return;

        this.fin = now;
        this.statut = estExpiree(now) ? StatutTentative.EXPIREE : StatutTentative.SOUMISE;

        // ✅ score total = auto + (manuel déjà saisi éventuellement)
        recalculerScoreTotal();
    }

    public boolean estExpiree(LocalDateTime now) {
        Objects.requireNonNull(now, "now ne doit pas être null");
        LocalDateTime deadline = calculerDeadline();
        return deadline != null && now.isAfter(deadline);
    }

    // ===== AUTO =====
    public double calculerScoreAuto() {
        double total = 0.0;
        for (ReponseDonnee rd : reponses) {
            if (rd.getQuestion() == null) continue;
            total += rd.getQuestion().corriger(rd.getContenu());
        }
        return total;
    }

    // ===== MANUEL (COURTE) =====

    // ✅ Méthode appelée par ton Main
    public void noterManuellement(UUID questionId, double note, String commentaire) {
        if (questionId == null) return;

        Question q = examen.trouverQuestion(questionId);
        if (q == null) throw new IllegalArgumentException("Question introuvable: " + questionId);

        // borne 0..barème
        double n = Math.max(0.0, Math.min(note, q.getBareme()));

        notesManuelles.put(questionId, n);

        if (commentaire == null || commentaire.isBlank()) {
            commentairesManuels.remove(questionId);
        } else {
            commentairesManuels.put(questionId, commentaire.trim());
        }

        // ✅ met à jour score total
        recalculerScoreTotal();
    }

    // ✅ (compat) ton ancien code peut continuer à l'utiliser
    public void appliquerNoteManuelle(UUID questionId, double note) {
        noterManuellement(questionId, note, null);
    }

    // ✅ Méthodes appelées par ton Main (pour afficher détail)
    public Double getNoteManuelle(UUID questionId) {
        return questionId == null ? null : notesManuelles.get(questionId);
    }

    public String getCommentaireManuel(UUID questionId) {
        return questionId == null ? null : commentairesManuels.get(questionId);
    }

    // ✅ score total visible étudiant/prof
    public double getScoreTotal() {
        return score;
    }

    private void recalculerScoreTotal() {
        double auto = calculerScoreAuto();
        double manuel = 0.0;
        for (Double v : notesManuelles.values()) {
            if (v != null) manuel += v;
        }
        this.score = auto + manuel;
    }

    // -------- Helpers privés --------
    private LocalDateTime calculerDeadline() {
        if (debut == null) return null;

        LocalDateTime deadlineParDuree = debut.plusMinutes(Math.max(0, examen.getDureeMinutes()));
        LocalDateTime finExamen = examen.getDateFin();

        if (finExamen == null) return deadlineParDuree;
        return deadlineParDuree.isBefore(finExamen) ? deadlineParDuree : finExamen;
    }

    private boolean peutModifier(LocalDateTime now) {
        if (statut != StatutTentative.EN_COURS) return false;
        if (debut == null) return false;
        if (estExpiree(now)) {
            this.statut = StatutTentative.EXPIREE;
            this.fin = now;
            return false;
        }
        return true;
    }

    private ReponseDonnee trouverReponse(UUID questionId) {
        if (questionId == null) return null;
        for (ReponseDonnee r : reponses) {
            if (r.getQuestion() != null && questionId.equals(r.getQuestion().getId())) return r;
        }
        return null;
    }

    // ---------- Getters ----------
    public UUID getId() { return id; }
    public LocalDateTime getDebut() { return debut; }
    public LocalDateTime getFin() { return fin; }
    public StatutTentative getStatut() { return statut; }
    public double getScore() { return score; } // maintenant = total
    public Examen getExamen() { return examen; }
    public Etudiant getEtudiant() { return etudiant; }
    public List<ReponseDonnee> getReponses() { return Collections.unmodifiableList(reponses); }
}
