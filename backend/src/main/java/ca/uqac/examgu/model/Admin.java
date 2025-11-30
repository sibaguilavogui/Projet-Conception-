package ca.uqac.examgu.model;

import ca.uqac.examgu.model.Enumerations.Role;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Admin extends Utilisateur {
    public Admin() {}
    public Admin(String email,
                      String motDePasse,
                      String prenom,
                      String nom,
                      LocalDate dateNaissance) {
        super(email, motDePasse, Role.ADMIN, prenom, nom, "STI", dateNaissance);
    }
}
