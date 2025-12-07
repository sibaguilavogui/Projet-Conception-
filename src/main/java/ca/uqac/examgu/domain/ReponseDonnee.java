package ca.uqac.examgu.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class ReponseDonnee {
    private final UUID id;
    private final Question question;
    private String contenu;
    private LocalDateTime dateMaj;

    // ==========================================================
    // ✅ CHAMPS AJOUTÉS POUR LA CORRECTION
    // ==========================================================
    private double noteAutomatique = 0.0;
    // La note manuelle est utilisée si l'enseignant corrige (prioritaire)
    private double noteManuelle = -1.0;
    private String commentaireCorrection;
    // ==========================================================

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

    // --- Getters et Setters pour la correction (AJOUTÉ) ---

    public double getNoteAutomatique() { return noteAutomatique; }
    public void setNoteAutomatique(double noteAutomatique) { this.noteAutomatique = noteAutomatique; }

    public double getNoteManuelle() { return noteManuelle; }
    public void setNoteManuelle(double noteManuelle) { this.noteManuelle = noteManuelle; }

    public String getCommentaireCorrection() { return commentaireCorrection; }
    public void setCommentaireCorrection(String commentaireCorrection) { this.commentaireCorrection = commentaireCorrection; }

    // --- Getters existants ---

    public UUID getId() { return id; }
    public Question getQuestion() { return question; }
    public String getContenu() { return contenu; }
    public LocalDateTime getDateMaj() { return dateMaj; }
}