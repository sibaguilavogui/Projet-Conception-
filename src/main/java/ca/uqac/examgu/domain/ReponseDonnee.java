package ca.uqac.examgu.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class ReponseDonnee {
    private final UUID id;
    private final Question question;
    private String contenu;
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
