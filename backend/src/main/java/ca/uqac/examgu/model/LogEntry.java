package ca.uqac.examgu.model;

import ca.uqac.examgu.model.Enumerations.TypeEvenement;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "logs")
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeEvenement type;

    @Column(name = "utilisateur", nullable = false)
    private String utilisateur;
    private String details;
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now(); // Initialisez par défaut

    public LogEntry() {}

    public LogEntry(TypeEvenement type, String utilisateur, String details) {
        this.type = type;
        this.utilisateur = utilisateur;
        this.details = details;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TypeEvenement getType() {
        return type;
    }

    public void setType(TypeEvenement type) {
        this.type = type;
    }

    public String getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(String utilisateur) {
        this.utilisateur = utilisateur;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}