package domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Représente l'inscription d'un étudiant à un examen.
 *
 * Attributs (UML) :
 *  - id : UUID
 *  - statut : StatutInscription
 *  - etudiant : Etudiant
 *  - examen : Examen
 *
 * Méthodes (UML) :
 *  + activer(): void
 *  + suspendre(): void
 *  + estActive(): boolean
 */
public class Inscription {

    private UUID id;
    private StatutInscription statut;
    private Etudiant etudiant;
    private Examen examen;

    // ---------- Constructeurs ----------

    public Inscription(Etudiant etudiant, Examen examen) {
        this(UUID.randomUUID(), StatutInscription.ACTIVE, etudiant, examen);
    }

    public Inscription(UUID id,
                       StatutInscription statut,
                       Etudiant etudiant,
                       Examen examen) {

        this.id = (id != null) ? id : UUID.randomUUID();
        this.statut = (statut != null) ? statut : StatutInscription.ACTIVE;
        this.etudiant = Objects.requireNonNull(etudiant, "etudiant ne doit pas être null");
        this.examen = Objects.requireNonNull(examen, "examen ne doit pas être null");
    }

    // ---------- Méthodes UML ----------

    public void activer() {
        this.statut = StatutInscription.ACTIVE;
    }

    public void suspendre() {
        this.statut = StatutInscription.SUSPENDUE;
    }

    public boolean estActive() {
        return this.statut == StatutInscription.ACTIVE;
    }

    // ---------- Getters / Setters ----------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = (id != null) ? id : UUID.randomUUID();
    }

    public StatutInscription getStatut() {
        return statut;
    }

    public void setStatut(StatutInscription statut) {
        this.statut = (statut != null) ? statut : this.statut;
    }

    public Etudiant getEtudiant() {
        return etudiant;
    }

    public void setEtudiant(Etudiant etudiant) {
        this.etudiant = Objects.requireNonNull(etudiant, "etudiant ne doit pas être null");
    }

    public Examen getExamen() {
        return examen;
    }

    public void setExamen(Examen examen) {
        this.examen = Objects.requireNonNull(examen, "examen ne doit pas être null");
    }
}
