package ca.uqac.examgu.model;

import ca.uqac.examgu.model.Enumerations.EtatExamen;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notes_publication")
public class NotePublication {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "est_publiee", nullable = false)
    private boolean estPubliee = false;

    @Column(name = "date_publication")
    private LocalDateTime datePublication;

    @Column(name = "date_retrait")
    private LocalDateTime dateRetrait;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examen_id", nullable = false, unique = true)
    @JsonIgnore
    private Examen examen;

    @Column(name = "message_publication")
    private String messagePublication;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    public NotePublication() {
        this.dateCreation = LocalDateTime.now();
    }

    public NotePublication(Examen examen, boolean estPublieeInitialement) {
        this();
        this.examen = Objects.requireNonNull(examen, "L'examen ne peut pas être null");
        this.estPubliee = estPublieeInitialement;

        if (estPublieeInitialement) {
            this.datePublication = LocalDateTime.now();
        }
    }

    public void publier() {
        publier(null);
    }

    public void publier(String message) {
        if (this.estPubliee) {
            throw new IllegalStateException("Les notes sont déjà publiées");
        }

        this.estPubliee = true;
        this.datePublication = LocalDateTime.now();
        this.dateRetrait = null;
        this.messagePublication = message;
        this.dateModification = LocalDateTime.now();
    }

    public void retirer() {
        if (!this.estPubliee) {
            throw new IllegalStateException("Les notes ne sont pas publiées");
        }

        this.estPubliee = false;
        this.dateRetrait = LocalDateTime.now();
        this.dateModification = LocalDateTime.now();
    }

    public boolean sontPubliees() {
        return estPubliee;
    }

    public boolean sontRetirees() {
        return !estPubliee && datePublication != null;
    }


    public String getStatutPublication() {
        if (estPubliee) {
            return "PUBLIÉE";
        } else if (datePublication != null) {
            return "RETIRÉE";
        } else {
            return "NON_PUBLIÉE";
        }
    }

    public boolean peutEtrePubliee() {
        return examen != null && examen.getEtat() == EtatExamen.FERME;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public boolean isEstPubliee() {
        return estPubliee;
    }

    public void setEstPubliee(boolean estPubliee) {
        this.estPubliee = estPubliee;
        this.dateModification = LocalDateTime.now();

        if (estPubliee && this.datePublication == null) {
            this.datePublication = LocalDateTime.now();
        } else if (!estPubliee && this.dateRetrait == null) {
            this.dateRetrait = LocalDateTime.now();
        }
    }

    public LocalDateTime getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(LocalDateTime datePublication) {
        this.datePublication = datePublication;
        this.dateModification = LocalDateTime.now();
    }

    public LocalDateTime getDateRetrait() {
        return dateRetrait;
    }

    public void setDateRetrait(LocalDateTime dateRetrait) {
        this.dateRetrait = dateRetrait;
        this.dateModification = LocalDateTime.now();
    }

    public Examen getExamen() {
        return examen;
    }

    public void setExamen(Examen examen) {
        this.examen = Objects.requireNonNull(examen, "L'examen ne peut pas être null");
        this.dateModification = LocalDateTime.now();
    }

    public String getMessagePublication() {
        return messagePublication;
    }

    public void setMessagePublication(String messagePublication) {
        this.messagePublication = messagePublication;
        this.dateModification = LocalDateTime.now();
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }

    // Méthodes utilitaires
    public String getResumePublication() {
        if (estPubliee && datePublication != null) {
            return String.format("Notes publiées le %s",
                    datePublication.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
        } else if (dateRetrait != null) {
            return String.format("Notes retirées le %s",
                    dateRetrait.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
        } else {
            return "Notes non publiées";
        }
    }

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        dateModification = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotePublication)) return false;
        NotePublication that = (NotePublication) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("NotePublication{id=%s, examen=%s, statut=%s}",
                id, examen != null ? examen.getTitre() : "null", getStatutPublication());
    }
}