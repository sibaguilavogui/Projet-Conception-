package domain;

public enum Role {
    ETUDIANT,
    ENSEIGNANT,
    ADMIN;

    public boolean peutGererUtilisateurs() {
        return this == ADMIN;
    }

    public boolean peutCreerExamens() {
        return this == ENSEIGNANT || this == ADMIN;
    }

    public boolean estEtudiant() {
        return this == ETUDIANT;
    }

    public boolean estEnseignant() {
        return this == ENSEIGNANT;
    }

    public boolean estAdmin() {
        return this == ADMIN;
    }
}

