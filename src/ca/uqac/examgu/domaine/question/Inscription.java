package ca.uqac.examgu.domaine.question;

import ca.uqac.examgu.domaine.examen.Examen;
import ca.uqac.examgu.domaine.utilisateur.Etudiant;

import java.util.Objects;
import java.util.UUID;

/**
 * Lien Examen ↔ Étudiant avec statut (ACTIVE, SUSPENDUE).
 * Méthodes : activer, suspendre, estActive.
 */
public class Inscription {

    private final UUID id;
    private StatutInscription statut;
    private final Etudiant etudiant;
    private final Examen examen;

    public Inscription(UUID id, Etudiant etudiant, Examen examen, StatutInscription statut) {
        if (id == null) throw new IllegalArgumentException("id requis");
        this.etudiant = Objects.requireNonNull(etudiant, "étudiant requis");
        this.examen   = Objects.requireNonNull(examen, "examen requis");
        this.statut   = Objects.requireNonNull(statut, "statut requis");
        this.id = id;
    }

    public void activer()   { this.statut = StatutInscription.ACTIVE; }
    public void suspendre() { this.statut = StatutInscription.SUSPENDUE; }
    public boolean estActive() { return this.statut == StatutInscription.ACTIVE; }

    // ===== Getters =====
    public UUID getId() { return id; }
    public StatutInscription getStatut() { return statut; }
    public Etudiant getEtudiant() { return etudiant; }
    public Examen getExamen() { return examen; }

    @Override public String toString() {
        return "Inscription{" + "id=" + id + ", statut=" + statut + '}';
    }
}
