package ca.uqac.examgu.model;

import ca.uqac.examgu.model.Enumerations.StatutInscription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "inscriptions")
public class Inscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private StatutInscription statut;

    @ManyToOne
    @JoinColumn(name = "etudiant_id")
    private Etudiant etudiant;

    @ManyToOne
    @JoinColumn(name = "examen_id")
    @JsonIgnore
    private Examen examen;

    public Inscription() {}

    public Inscription(StatutInscription statut, Etudiant etudiant, Examen examen) {
        this.statut = (statut != null) ? statut : StatutInscription.ACTIVE;
        this.etudiant = Objects.requireNonNull(etudiant, "etudiant ne doit pas être null");
        this.examen = Objects.requireNonNull(examen, "examen ne doit pas être null");
    }

    public void activer() {
        this.statut = StatutInscription.ACTIVE;
    }

    public void suspendre() {
        this.statut = StatutInscription.SUSPENDUE;
    }

    public boolean estActive() {
        return this.statut == StatutInscription.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StatutInscription getStatut() {
        return statut;
    }

    public void setStatut(StatutInscription statut) {
        this.statut = statut;
    }

    public Etudiant getEtudiant() {
        return etudiant;
    }

    public void setEtudiant(Etudiant etudiant) {
        this.etudiant = etudiant;
    }

    public Examen getExamen() {
        return examen;
    }

    public void setExamen(Examen examen) {
        this.examen = examen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Inscription)) return false;
        Inscription that = (Inscription) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Inscription{id=%s, etudiant=%s, examen=%s, statut=%s}",
                id, etudiant != null ? etudiant.getEmail() : "null",
                examen != null ? examen.getTitre() : "null", statut);
    }
}