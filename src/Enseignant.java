package domain;

import java.util.UUID;

public class Enseignant extends Utilisateur {

    private String numeroEmploye;
    private String departement; // ex: "Informatique et mathématique"

    public Enseignant() {
        super();
        setRole(Role.ENSEIGNANT);
    }

    public Enseignant(UUID id,
                      String email,
                      String motDePasseHash,
                      String prenom,
                      String nom,
                      String numeroEmploye,
                      String departement) {
        super(id, email, motDePasseHash, Role.ENSEIGNANT, prenom, nom);
        this.numeroEmploye = numeroEmploye;
        this.departement = departement;
    }

    public String getNumeroEmploye() {
        return numeroEmploye;
    }

    public void setNumeroEmploye(String numeroEmploye) {
        this.numeroEmploye = numeroEmploye;
    }

    public String getDepartement() {
        return departement;
    }

    public void setDepartement(String departement) {
        this.departement = departement;
    }
}

