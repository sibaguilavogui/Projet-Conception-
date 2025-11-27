package domain;

public class Admin extends Utilisateur {
    public Admin(String email, String motDePasseEnClair) {
        super(email, motDePasseEnClair, Role.ADMIN);
    }
}
