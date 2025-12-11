package ca.uqac.examgu.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.Hibernate;

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
    @JsonIgnore
    private Question question;

    @Column(length = 2000)
    private String contenu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tentative_id", nullable = false)
    @JsonIgnore
    private Tentative tentative;

    @Column(name = "date_maj")
    private LocalDateTime dateMaj;

    @Column(name = "note_partielle")
    private double notePartielle = 0.0;

    @Column(name = "commentaire")
    private String commentaire;

    @Column(name = "est_corrigee")
    private boolean estCorrigee = false;

    @Column(name = "date_correction")
    private LocalDateTime dateCorrection;

    @Column(name = "auto_corrigee")
    private boolean autoCorrigee;


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
    }

    public void mettreAJourContenu(String contenu) {
        if (!this.estCorrigee){
            LocalDateTime now = LocalDateTime.now();
            this.contenu = contenu != null ? contenu : "";
            this.dateMaj = now;
        }
    }

    public void noterPatiellement(double note) {
        noterPatiellement(note, null);
    }

    public void noterPatiellement(double note, String commentaire) {

        if (note < 0) {
            throw new IllegalArgumentException("La note ne peut pas être négative");
        }
        if (note > question.getBareme()) {
            throw new IllegalArgumentException(
                    String.format("La note ne peut pas dépasser le barème de la question (%.1f)",
                            question.getBareme()));
        }

        setNotePartielle(note);
        this.commentaire = commentaire;
        setEstCorrigee(true);
        this.dateCorrection = LocalDateTime.now();
    }

    public void corrigerAutomatiquement() throws Exception {
        if (question.getType().equals("CHOIX")) {
            QuestionAChoix questionChoix = (QuestionAChoix) Hibernate.unproxy(question);
            double note = questionChoix.calculerNote(this);
            noterPatiellement(note);
        }
    }

    public void annulerCorrection() {
        this.notePartielle = 0.0;
        this.estCorrigee = false;
        this.dateCorrection = null;
    }

    public boolean estQuestionDeveloppement() {
        return question.getType().equals("DEVELOPPEMENT");
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