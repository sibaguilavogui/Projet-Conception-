package domain;

public enum EtatExamen {
    BROUILLON,
    OUVERT,
    FERME;

    public boolean estModifiable() {
        return this == BROUILLON;
    }

    public boolean estVisiblePourEtudiants() {
        return this == OUVERT;
    }

    public boolean estTermine() {
        return this == FERME;
    }
}


