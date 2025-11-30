package ca.uqac.examgu.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "reponses_donnees")
public class ReponseDonnee {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(length = 2000)
    private String contenu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tentative_id", nullable = false)
    private Tentative tentative;

    @Column(name = "date_maj")
    private LocalDateTime dateMaj;

    @Column(name = "note_partielle")
    private double notePartielle = 0.0;

    @Column(name = "est_corrigee")
    private boolean estCorrigee = false;

    @Column(name = "date_correction")
    private LocalDateTime dateCorrection;

    @Column(name = "auto_corrigee")
    private boolean autoCorrigee = false;

    public ReponseDonnee() {
    }

    public ReponseDonnee(Question question) {
        this(question, "", LocalDateTime.now());
    }

    public ReponseDonnee(Question question, String contenu, LocalDateTime dateMaj) {
        this.question = Objects.requireNonNull(question, "La question ne peut pas être null");
        this.contenu = contenu != null ? contenu : "";
        this.dateMaj = dateMaj != null ? dateMaj : LocalDateTime.now();
        this.notePartielle = 0.0;
        this.estCorrigee = false;
    }

    public void mettreAJourContenu(String contenu) {
        mettreAJourContenu(contenu, LocalDateTime.now());
    }

    public void mettreAJourContenu(String contenu, LocalDateTime now) {
        this.contenu = contenu != null ? contenu : "";
        this.dateMaj = now != null ? now : LocalDateTime.now();
        if (this.estCorrigee && !this.autoCorrigee) {
            this.estCorrigee = false;
            this.notePartielle = 0.0;
            this.dateCorrection = null;
        }
    }

    public void noterPatiellement(double note) {
        noterPatiellement(note, null, false);
    }

    public void noterPatiellement(double note, String commentaire) {
        noterPatiellement(note, commentaire, false);
    }

    public void noterPatiellement(double note, String commentaire, boolean autoCorrigee) {

        if (note < 0) {
            throw new IllegalArgumentException("La note ne peut pas être négative");
        }
        if (note > question.getBareme()) {
            throw new IllegalArgumentException(
                    String.format("La note ne peut pas dépasser le barème de la question (%.1f)",
                            question.getBareme()));
        }

        this.notePartielle = note;
        this.estCorrigee = true;
        this.autoCorrigee = autoCorrigee;
        this.dateCorrection = LocalDateTime.now();
    }

    public void corrigerAutomatiquement() {
        if (question instanceof QuestionAChoix) {
            QuestionAChoix questionChoix = (QuestionAChoix) question;
            double note = questionChoix.calculerNote(this);
            noterPatiellement(note, "Correction automatique", true);
        }
    }

    public void annulerCorrection() {
        this.notePartielle = 0.0;
        this.estCorrigee = false;
        this.autoCorrigee = false;
        this.dateCorrection = null;
    }

    public boolean estQuestionDeveloppement() {
        return question instanceof QuestionADeveloppement;
    }


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = Objects.requireNonNull(question, "La question ne peut pas être null");
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu != null ? contenu : "";
        this.dateMaj = LocalDateTime.now();
    }

    public Tentative getTentative() {
        return tentative;
    }

    public void setTentative(Tentative tentative) {
        this.tentative = tentative;
    }

    public LocalDateTime getDateMaj() {
        return dateMaj;
    }

    public void setDateMaj(LocalDateTime dateMaj) {
        this.dateMaj = dateMaj;
    }

    public double getNotePartielle() {
        return notePartielle;
    }

    public void setNotePartielle(double notePartielle) {
        if (notePartielle < 0) {
            throw new IllegalArgumentException("La note ne peut pas être négative");
        }
        this.notePartielle = notePartielle;
        if (notePartielle > 0) {
            this.estCorrigee = true;
            if (this.dateCorrection == null) {
                this.dateCorrection = LocalDateTime.now();
            }
        }
    }

    public boolean isEstCorrigee() {
        return estCorrigee;
    }

    public void setEstCorrigee(boolean estCorrigee) {
        this.estCorrigee = estCorrigee;
        if (estCorrigee && this.dateCorrection == null) {
            this.dateCorrection = LocalDateTime.now();
        }
    }

    public LocalDateTime getDateCorrection() {
        return dateCorrection;
    }

    public void setDateCorrection(LocalDateTime dateCorrection) {
        this.dateCorrection = dateCorrection;
    }

    public boolean isAutoCorrigee() {
        return autoCorrigee;
    }

    public void setAutoCorrigee(boolean autoCorrigee) {
        this.autoCorrigee = autoCorrigee;
    }

    // Méthodes de cycle de vie JPA
    @PrePersist
    protected void onCreate() {
        if (dateMaj == null) {
            dateMaj = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dateMaj = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReponseDonnee)) return false;
        ReponseDonnee that = (ReponseDonnee) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("ReponseDonnee{id=%s, question=%s, corrigee=%s, note=%.1f/%.1f}",
                id,
                question != null ? question.getId() : "null",
                estCorrigee,
                notePartielle,
                question != null ? question.getBareme() : 0.0);
    }
}