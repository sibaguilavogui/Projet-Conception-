package domain;

public enum TypeQuestion {
    QCM,
    VRAI_FAUX,
    COURTE;

    public boolean estAutoCorrigeable() {
        // COURTE nécessite souvent une correction humaine
        return this == QCM || this == VRAI_FAUX;
    }
}

