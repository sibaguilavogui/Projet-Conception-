package ca.uqac.examgu.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class Inscription {
    private final Etudiant etudiant;
    private final Examen examen;
    private boolean active = true;
    private final LocalDateTime dateInscription = LocalDateTime.now();

    public Inscription(Etudiant etudiant, Examen examen) {
        this.etudiant = Objects.requireNonNull(etudiant);
        this.examen = Objects.requireNonNull(examen);
    }

    public boolean estActive() { return active; }
    public void desactiver() { this.active = false; }
    public void activer() { this.active = true; }

    public Etudiant getEtudiant() { return etudiant; }
    public Examen getExamen() { return examen; }
    public LocalDateTime getDateInscription() { return dateInscription; }
}
