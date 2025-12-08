package ca.uqac.examgu.model;

import ca.uqac.examgu.dto.ExamenDTO;
import ca.uqac.examgu.model.Enumerations.Role;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "enseignants")
public class Enseignant extends Utilisateur {

    public Enseignant() {
        super();
        this.setRole(Role.ENSEIGNANT);
    }

    public Enseignant(String email, String motDePasse, String prenom, String nom,
                      String departement, LocalDate dateNaissance) {
        super(email, motDePasse, Role.ENSEIGNANT, prenom, nom, departement, dateNaissance);
    }

    public Examen creerExamen(ExamenDTO ex) {
        return new Examen(ex.getTitre(), ex.getDescription(), ex.getDateDebut(), ex.getDateFin(),
                ex.getDureeMinutes(), this);
    }

}