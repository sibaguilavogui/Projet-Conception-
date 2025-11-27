package ca.uqac.examgu.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
public class Inscription {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "etudiant_id")
    private final Etudiant etudiant;

    @ManyToOne
    @JoinColumn(name = "examen_id")
    private final Examen examen;
    private boolean active = true;
    private final LocalDateTime dateInscription = LocalDateTime.now();

    public Inscription(UUID id, Etudiant etudiant, Examen examen) {
        this.id = id;
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
