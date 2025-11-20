package domain;

import java.util.UUID;

public class Etudiant extends Utilisateur {

    private String numeroEtudiant;
    private String programme;   // ex: "Génie informatique"

    public Etudiant() {
        super();
        setRole(Role.ETUDIANT);
    }

    public Etudiant(UUID id,
                    String email,
                    String motDePasseHash,
                    String prenom,
                    String nom,
                    String numeroEtudiant,
                    String programme) {
        super(id, email, motDePasseHash, Role.ETUDIANT, prenom, nom);
        this.numeroEtudiant = numeroEtudiant;
        this.programme = programme;
    }

    public String getNumeroEtudiant() {
        return numeroEtudiant;
    }

    public void setNumeroEtudiant(String numeroEtudiant) {
        this.numeroEtudiant = numeroEtudiant;
    }

    public String getProgramme() {
        return programme;
    }

    public void setProgramme(String programme) {
        this.programme = programme;
    }

    public boolean estDansProgramme(String programme) {
        return this.programme != null && this.programme.equalsIgnoreCase(programme);
    }
}


