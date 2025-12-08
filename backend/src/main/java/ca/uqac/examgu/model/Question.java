package ca.uqac.examgu.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "questions")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 1000)
    private String enonce;

    @Column(nullable = false)
    private double bareme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examen_id")
    @JsonIgnore
    private Examen examen;

    @Column(name = "date_creation", nullable = false)
    private java.time.LocalDateTime dateCreation;

    protected Question() {
        this.dateCreation = java.time.LocalDateTime.now();
    }

    protected Question(String enonce, double bareme, Examen examen) {
        this.dateCreation = java.time.LocalDateTime.now();
        this.enonce=enonce;
        this.bareme=bareme;
        this.examen=examen;
    }

    public String getTypeQuestion() {
        if (this instanceof QuestionAChoix) {
            QuestionAChoix qac = (QuestionAChoix) this;
            return qac.getTypeChoix().toString();
        } else if (this instanceof QuestionADeveloppement) {
            return "DEVELOPPEMENT";
        }
        return "INCONNU";
    }

    public abstract String getType();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEnonce() {
        return enonce;
    }

    public void setEnonce(String enonce) {
        this.enonce = Objects.requireNonNull(enonce, "L'énoncé ne peut pas être null");
    }

    public double getBareme() {
        return bareme;
    }

    public void setBareme(double bareme) {
        if (bareme <= 0) {
            throw new IllegalArgumentException("Le barème doit être positif");
        }
        this.bareme = bareme;
    }

    public Examen getExamen() {
        return examen;
    }

    public void setExamen(Examen examen) {
        this.examen = examen;
    }

    public java.time.LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(java.time.LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    // Méthodes de cycle de vie JPA
    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = java.time.LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
    }

    // Égalité et hashcode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Question)) return false;
        Question question = (Question) o;
        return Objects.equals(id, question.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Question{id=%s, enonce='%s', bareme=%.1f, type=%s}",
                id, enonce, bareme, getTypeQuestion());
    }
}