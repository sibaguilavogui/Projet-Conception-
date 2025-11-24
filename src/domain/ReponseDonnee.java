package domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReponseDonnee {
    private final UUID id;
    private Question question;
    private String contenu;
    private double notePartielle;
    private LocalDateTime derniereSauvegarde;

    public ReponseDonnee(Question question) {
        this.id = UUID.randomUUID();
        this.question = question;
        this.contenu = "";
        this.notePartielle = 0.0;
        this.derniereSauvegarde = null;
    }

    public UUID getId() {
        return id;
    }
    public Question getQuestion() {
        return question;
    }
    public String getContenu() {
        return contenu;
    }

    public void mettreAJourContenu(String nouveau) {
        this.contenu = nouveau;
        this.derniereSauvegarde = LocalDateTime.now();
    }

    public double getNotePartielle() {
        return notePartielle;
    }

    public void noterPatiellement(double note) {
        this.notePartielle = note;
    }

    public LocalDateTime getDerniereSauvegarde() {
        return derniereSauvegarde;
    }

}

