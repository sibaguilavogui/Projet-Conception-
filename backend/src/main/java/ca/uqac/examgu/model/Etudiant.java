package ca.uqac.examgu.model;

import ca.uqac.examgu.model.Enumerations.Role;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity
public class Etudiant extends Utilisateur {

    public Etudiant(){}

    public Etudiant(String email,
                    String motDePasseEnClair,
                    String prenom,
                    String nom,
                    String departement,
                    LocalDate dateNaissance) {
        super(email, motDePasseEnClair, Role.ETUDIANT, prenom, nom, departement, dateNaissance);
    }
}
