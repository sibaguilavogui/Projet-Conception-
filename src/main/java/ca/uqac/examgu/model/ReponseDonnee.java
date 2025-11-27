package ca.uqac.examgu.model;

import jakarta.persistence.*;
import jakarta.persistence.GenerationType;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
public class ReponseDonnee {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private final Question question;
    private String contenu;

    @ManyToOne
    @JoinColumn(name = "tentative_id") // colonne FK dans MySQL
    private Tentative tentative;
    private LocalDateTime dateMaj;

    public ReponseDonnee(Question question) {
        this(UUID.randomUUID(), question, null, null);
    }

    public ReponseDonnee(UUID id, Question question, String contenu, LocalDateTime dateMaj) {
        this.id = (id != null) ? id : UUID.randomUUID();
        this.question = Objects.requireNonNull(question, "question ne doit pas être null");
        this.contenu = contenu;
        this.dateMaj = dateMaj;
    }

    public void mettreAJourContenu(String contenu, LocalDateTime now) {
        this.contenu = contenu;
        this.dateMaj = now;
    }

    public UUID getId() { return id; }
    public Question getQuestion() { return question; }
    public String getContenu() { return contenu; }
    public LocalDateTime getDateMaj() { return dateMaj; }
}
