package ca.uqac.examgu.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class NotePublication {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    private boolean publiee = false;
    private LocalDateTime datePublication;

    @OneToOne(mappedBy = "publication")
    @JoinColumn(name = "examen_id")
    private Examen examen;

    public NotePublication(UUID id) {
        this.id = id;
    }

    public void publier(LocalDateTime now) {
        this.publiee = true;
        this.datePublication = now;
    }

    public boolean estPubliee() {
        return publiee;
    }

    public LocalDateTime getDatePublication() {
        return datePublication;
    }
}
