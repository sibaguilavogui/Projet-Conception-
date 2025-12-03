package ca.uqac.examgu.domain;

import java.time.LocalDate;

public class Admin extends Utilisateur {

    // ✅ constructeur principal
    public Admin(String email,
                 String motDePasse,
                 String prenom,
                 String nom,
                 String departement,
                 LocalDate dateNaissance) {

        // ordre conforme au constructeur de Utilisateur
        super(email, motDePasse, Role.ADMIN, prenom, nom, departement, dateNaissance);
    }

    // ✅ constructeur pratique SANS date (pour le bootstrap)
    public Admin(String email,
                 String motDePasse,
                 String prenom,
                 String nom,
                 String departement) {

        this(email, motDePasse, prenom, nom, departement, LocalDate.of(1990, 1, 1));
    }
}
