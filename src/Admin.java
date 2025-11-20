package domain;

import java.util.UUID;

public class Admin extends Utilisateur {

    // Exemple : super administrateur qui peut tout faire
    private boolean superAdmin;

    public Admin() {
        super();
        setRole(Role.ADMIN);
    }

    public Admin(UUID id,
                 String email,
                 String motDePasseHash,
                 String prenom,
                 String nom,
                 boolean superAdmin) {
        super(id, email, motDePasseHash, Role.ADMIN, prenom, nom);
        this.superAdmin = superAdmin;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public void setSuperAdmin(boolean superAdmin) {
        this.superAdmin = superAdmin;
    }

    public boolean peutToutGerer() {
        return superAdmin;
    }
}

