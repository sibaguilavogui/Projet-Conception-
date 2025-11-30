package ca.uqac.examgu.model;

import ca.uqac.examgu.model.Enumerations.EtatExamen;
import ca.uqac.examgu.model.Enumerations.Role;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "enseignants")
public class Enseignant extends Utilisateur {

    // Constructeur par défaut nécessaire pour JPA/Hibernate
    public Enseignant() {
        super();
        this.setRole(Role.ENSEIGNANT);
    }

    // Constructeur complet
    public Enseignant(String email, String motDePasse, String prenom, String nom,
                      String departement, LocalDate dateNaissance) {
        super(email, motDePasse, Role.ENSEIGNANT, prenom, nom, departement, dateNaissance);
    }

    public Examen creerExamen(String titre) {
        Objects.requireNonNull(titre, "Le titre de l'examen ne peut pas être null");

        return new Examen(titre, this);
    }

    public void planifierExamen(Examen examen, LocalDate dateDebut, LocalDate dateFin, int dureeMinutes) {
        Objects.requireNonNull(examen, "L'examen ne peut pas être null");
        Objects.requireNonNull(dateDebut, "La date de début ne peut pas être null");
        Objects.requireNonNull(dateFin, "La date de fin ne peut pas être null");

        if (!examen.getCreateur().equals(this)) {
            throw new SecurityException("Seul le créateur de l'examen peut le planifier");
        }

        if (dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("La date de fin doit être après la date de début");
        }

        if (dureeMinutes <= 0) {
            throw new IllegalArgumentException("La durée doit être positive");
        }

        examen.planifier(
                dateDebut.atStartOfDay(),
                dateFin.atTime(23, 59, 59),
                dureeMinutes
        );
    }


    public void publierNotes(Examen examen) {
        Objects.requireNonNull(examen, "L'examen ne peut pas être null");

        if (!examen.getCreateur().equals(this)) {
            throw new SecurityException("Seul le créateur de l'examen peut publier les notes");
        }

        if (examen.getPublication() == null) {
            examen.setPublication(new NotePublication(examen, true));
        } else {
            examen.getPublication().publier();
        }
    }

    public boolean peutModifierExamen(Examen examen) {
        return examen.getCreateur().equals(this) &&
                examen.getEtat() == EtatExamen.BROUILLON;
    }

    public boolean peutConsulterTentatives(Examen examen) {
        return examen.getCreateur().equals(this) &&
                examen.getEtat() == EtatExamen.FERME;
    }
}