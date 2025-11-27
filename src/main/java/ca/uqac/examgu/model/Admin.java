package ca.uqac.examgu.model;

import jakarta.persistence.*;

;

@Entity
public class Admin extends Utilisateur {
    public Admin(String email, String motDePasseEnClair) {
        super(email, motDePasseEnClair, Role.ADMIN);
    }
}
