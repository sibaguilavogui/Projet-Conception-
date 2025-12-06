package ca.uqac.examgu.model;

import ca.uqac.examgu.model.Enumerations.StatutTentative;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "tentatives")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "hibernateProxy", "persistentBag"})
public class Tentative {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private LocalDateTime debut;

    @Column(name = "date_fin")
    private LocalDateTime fin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTentative statut;

    @Column(nullable = false)
    private double score;

    @Column(name = "note_finale")
    private double noteFinale = 0.0;

    @Column(name = "est_note_finale_calculee")
    private boolean estNoteFinaleCalculee = false;

    @Column(name = "date_calcul_note")
    private LocalDateTime dateCalculNote;



    @Column(name = "est_corrigee")
    private boolean estCorrigee = false;

    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examen_id", nullable = false)
    @JsonIgnore
    private Examen examen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @OneToMany(mappedBy = "tentative", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ReponseDonnee> reponses = new ArrayList<>();

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    // Constructeurs
    public Tentative() {
        this.dateCreation = LocalDateTime.now();
        this.statut = StatutTentative.EN_COURS;
        this.score = 0.0;
    }

    public Tentative(Examen examen, Etudiant etudiant) {
        this();
        this.examen = Objects.requireNonNull(examen, "L'examen ne peut pas être null");
        this.etudiant = Objects.requireNonNull(etudiant, "L'étudiant ne peut pas être null");
        this.debut = LocalDateTime.now();
        this.fin = calculerDateFin();
    }

    // Méthodes métier principales
    public void demarrer() {
        LocalDateTime now = LocalDateTime.now();

        this.debut = now;
        this.fin = calculerDateFin();
        this.statut = StatutTentative.EN_COURS;
        this.dateModification = now;
    }

    public void sauvegarderReponse(UUID questionId, String contenu) {
        LocalDateTime now = LocalDateTime.now();

        // Vérifier si la tentative peut encore être modifiée
        if (!peutModifier()) {
            throw new IllegalStateException("La tentative ne peut plus être modifiée (soumise ou deadline atteinte)");
        }

        Question question = examen.trouverQuestion(questionId);
        if (question == null) {
            throw new IllegalArgumentException("Question non trouvée: " + questionId);
        }

        ReponseDonnee reponse = trouverReponse(questionId);
        if (reponse == null) {
            reponse = new ReponseDonnee(question, contenu, now);
            reponse.setTentative(this);
            reponses.add(reponse);
        } else {
            reponse.mettreAJourContenu(contenu, now);
        }

        this.dateModification = now;
    }

    public void soumettre() {
        LocalDateTime now = LocalDateTime.now();

        if (statut != StatutTentative.EN_COURS) {
            throw new IllegalStateException("Seules les tentatives en cours peuvent être soumises");
        }

        this.fin = now;
        this.dateSoumission = now;
        this.statut = StatutTentative.SOUMISE;

        // Corriger automatiquement les questions QCM
        corrigerAutomatiquement();

        // Recalculer le score total
        recalculerScoreTotal();
        this.dateModification = now;
    }

    public int tempsRestant() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime deadline = calculerDeadline();
        if (deadline == null || now.isAfter(deadline)) {
            return 0;
        }

        long seconds = Duration.between(now, deadline).getSeconds();
        return (int) Math.max(0, seconds);
    }

    public boolean estExpiree() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = calculerDeadline();

        // Vérifier si la deadline est dépassée
        if (deadline != null && now.isAfter(deadline)) {
            // Si la tentative est encore en cours, elle doit être soumise automatiquement
            if (statut == StatutTentative.EN_COURS) {
                return true;
            }
        }
        return false;
    }

    private void corrigerAutomatiquement() {
        for (ReponseDonnee reponse : reponses) {
            if (reponse.getQuestion() instanceof QuestionAChoix && !reponse.isEstCorrigee()) {
                reponse.corrigerAutomatiquement();
            }
        }
    }

    public void noterManuellement(UUID questionId, double note, String commentaire) {
        if (questionId == null) {
            throw new IllegalArgumentException("L'ID de la question ne peut pas être null");
        }

        Question question = examen.trouverQuestion(questionId);
        if (question == null) {
            throw new IllegalArgumentException("Question introuvable: " + questionId);
        }

        if (!(question instanceof QuestionADeveloppement)) {
            throw new IllegalArgumentException("Seules les questions à développement peuvent être notées manuellement");
        }

        ReponseDonnee reponse = trouverReponse(questionId);
        if (reponse == null) {
            reponse = new ReponseDonnee(question, "", LocalDateTime.now());
            reponse.setTentative(this);
            reponses.add(reponse);
        }

        reponse.noterPatiellement(note, commentaire, false);
        recalculerScoreTotal();
        this.dateModification = LocalDateTime.now();
    }

    public void appliquerNoteManuelle(UUID questionId, double note) {
        noterManuellement(questionId, note, null);
    }

    public void recalculerScoreTotal() {
        this.score = reponses.stream()
                .mapToDouble(ReponseDonnee::getNotePartielle)
                .sum();
    }

    public double calculerScoreAuto() {
        return reponses.stream()
                .filter(r -> r.getQuestion() instanceof QuestionAChoix)
                .mapToDouble(r -> {
                    if (r.getQuestion() instanceof QuestionAChoix) {
                        QuestionAChoix question = (QuestionAChoix) r.getQuestion();
                        return question.calculerNote(r);
                    }
                    return 0.0;
                })
                .sum();
    }

    public void calculerNoteFinale() {
        this.noteFinale = this.reponses.stream()
                .mapToDouble(ReponseDonnee::getNotePartielle)
                .sum();
        this.estNoteFinaleCalculee = true;
        this.dateCalculNote = LocalDateTime.now();
    }

    private LocalDateTime calculerDeadline() {
        if (debut == null) {
            return null;
        }

        // La deadline est soit la date de fin de l'examen, soit le début + durée de l'examen
        LocalDateTime deadlineParDuree = debut.plusMinutes(Math.max(0, examen.getDureeMinutes()));
        LocalDateTime finExamen = examen.getDateFin();

        if (finExamen == null) {
            return deadlineParDuree;
        }

        // Retourner la plus proche des deux dates
        return deadlineParDuree.isBefore(finExamen) ? deadlineParDuree : finExamen;
    }

    private LocalDateTime calculerDateFin() {
        if (debut == null) {
            return null;
        }
        return debut.plusMinutes(Math.max(0, examen.getDureeMinutes()));
    }

    private boolean peutModifier() {
        // Une tentative ne peut être modifiée que si elle est en cours ET non expirée
        return statut == StatutTentative.EN_COURS && !estExpiree();
    }

    private ReponseDonnee trouverReponse(UUID questionId) {
        return reponses.stream()
                .filter(r -> r.getQuestion() != null && questionId.equals(r.getQuestion().getId()))
                .findFirst()
                .orElse(null);
    }

    public ReponseDonnee getReponsePourQuestion(UUID questionId) {
        return trouverReponse(questionId);
    }

    public boolean estCompletee() {
        return statut == StatutTentative.SOUMISE;
    }

    public boolean peutEtreCorrigee() {
        return estCompletee() && !estCorrigee;
    }

    public int getNombreReponses() {
        return reponses.size();
    }

    public int getNombreQuestionsRepondues() {
        return (int) reponses.stream()
                .filter(r -> r.getContenu() != null && !r.getContenu().trim().isEmpty())
                .count();
    }

    public double getPourcentageCompletion() {
        int totalQuestions = examen.getQuestions().size();
        return totalQuestions > 0 ? (double) getNombreQuestionsRepondues() / totalQuestions * 100 : 0;
    }

    public boolean peutEtreReprise() {
        return statut == StatutTentative.EN_COURS && !estExpiree();
    }

    public void mettreAJourDateModification() {
        this.dateModification = LocalDateTime.now();
    }

    public Map<String, Object> getResumeProgression() {
        Map<String, Object> resume = new HashMap<>();
        resume.put("id", id);
        resume.put("nombreQuestions", examen.getQuestions().size());
        resume.put("nombreReponses", getNombreReponses());
        resume.put("pourcentageCompletion", getPourcentageCompletion());
        resume.put("tempsRestant", tempsRestant());
        resume.put("statut", statut);
        resume.put("score", score);
        return resume;
    }

    // Getters et Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDateTime getDebut() {
        return debut;
    }

    public void setDebut(LocalDateTime debut) {
        this.debut = debut;
        this.dateModification = LocalDateTime.now();
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public void setFin(LocalDateTime fin) {
        this.fin = fin;
        this.dateModification = LocalDateTime.now();
    }

    public StatutTentative getStatut() {
        return statut;
    }

    public void setStatut(StatutTentative statut) {
        this.statut = statut;
        this.dateModification = LocalDateTime.now();
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
        this.dateModification = LocalDateTime.now();
    }

    public boolean estCorrigee() {
        return estCorrigee;
    }

    public void setCorrigee(boolean estCorrigee) {
        this.estCorrigee = estCorrigee;
        this.dateModification = LocalDateTime.now();
    }

    public LocalDateTime getDateSoumission() {
        return dateSoumission;
    }

    public void setDateSoumission(LocalDateTime dateSoumission) {
        this.dateSoumission = dateSoumission;
    }

    public Examen getExamen() {
        return examen;
    }

    public void setExamen(Examen examen) {
        this.examen = examen;
        this.dateModification = LocalDateTime.now();
    }

    public Etudiant getEtudiant() {
        return etudiant;
    }

    public void setEtudiant(Etudiant etudiant) {
        this.etudiant = etudiant;
        this.dateModification = LocalDateTime.now();
    }

    public List<ReponseDonnee> getReponses() {
        return Collections.unmodifiableList(reponses);
    }

    public void setReponses(List<ReponseDonnee> reponses) {
        this.reponses.clear();
        if (reponses != null) {
            this.reponses.addAll(reponses);
            this.reponses.forEach(r -> r.setTentative(this));
        }
        this.dateModification = LocalDateTime.now();
    }

    public void addReponse(ReponseDonnee reponse) {
        if (reponse != null) {
            reponse.setTentative(this);
            this.reponses.add(reponse);
            this.dateModification = LocalDateTime.now();
        }
    }

    public void removeReponse(ReponseDonnee reponse) {
        if (reponse != null) {
            this.reponses.remove(reponse);
            this.dateModification = LocalDateTime.now();
        }
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }

    public double getNoteFinale() {
        return noteFinale;
    }

    public void setNoteFinale(double noteFinale) {
        this.noteFinale = noteFinale;
        this.estNoteFinaleCalculee = true;
        this.dateCalculNote = LocalDateTime.now();
    }

    public boolean isEstNoteFinaleCalculee() {
        return estNoteFinaleCalculee;
    }

    public void setEstNoteFinaleCalculee(boolean estNoteFinaleCalculee) {
        this.estNoteFinaleCalculee = estNoteFinaleCalculee;
    }

    public LocalDateTime getDateCalculNote() {
        return dateCalculNote;
    }

    public void setDateCalculNote(LocalDateTime dateCalculNote) {
        this.dateCalculNote = dateCalculNote;
    }

    public Map<String, Object> getInfosPourComposition() {
        Map<String, Object> infos = new HashMap<>();

        infos.put("id", id);
        infos.put("debut", debut);
        infos.put("fin", fin);
        infos.put("statut", statut);
        infos.put("estExpiree", estExpiree());
        infos.put("tempsRestant", tempsRestant());
        infos.put("score", score);
        infos.put("estCorrigee", estCorrigee);
        infos.put("dateSoumission", dateSoumission);

        // Infos sur l'étudiant (limitées)
        Map<String, Object> etudiantInfo = new HashMap<>();
        etudiantInfo.put("id", etudiant.getId());
        etudiantInfo.put("nomComplet", etudiant.getNom() + " " + etudiant.getPrenom());
        infos.put("etudiant", etudiantInfo);

        // Infos sur l'examen (limitées)
        Map<String, Object> examenInfo = new HashMap<>();
        examenInfo.put("id", examen.getId());
        examenInfo.put("titre", examen.getTitre());
        examenInfo.put("description", examen.getDescription());
        examenInfo.put("dureeMinutes", examen.getDureeMinutes());
        examenInfo.put("nombreQuestions", examen.getQuestions().size());
        examenInfo.put("barèmeTotal", examen.totalPoints());
        infos.put("examen", examenInfo);

        // Progression
        infos.put("nombreQuestionsRepondues", getNombreQuestionsRepondues());
        infos.put("pourcentageCompletion", getPourcentageCompletion());

        return infos;
    }

    // Méthodes de cycle de vie JPA
    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        dateModification = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tentative)) return false;
        Tentative tentative = (Tentative) o;
        return Objects.equals(id, tentative.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Tentative{id=%s, etudiant=%s, examen=%s, statut=%s, score=%.1f}",
                id,
                etudiant != null ? etudiant.getEmail() : "null",
                examen != null ? examen.getTitre() : "null",
                statut,
                score);
    }
}