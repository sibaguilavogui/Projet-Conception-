package domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Examen {

    private UUID id;
    private String titre;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private int dureeMinutes;
    private EtatExamen etat;       // enum dans le même package domain
    private UUID createurId;

    private final List<Question> questions;
    private final List<Inscription> inscriptions;
    private NotePublication publication;

    public Examen() {
        this.id = UUID.randomUUID();
        this.questions = new ArrayList<>();
        this.inscriptions = new ArrayList<>();
        this.etat = EtatExamen.BROUILLON;
    }

    public Examen(UUID id, String titre, UUID createurId) {
        this();
        this.id = (id != null) ? id : UUID.randomUUID();
        this.titre = titre;
        this.createurId = createurId;
    }

    public void planifier(LocalDateTime debut, LocalDateTime fin, int dureeMinutes) {
        Objects.requireNonNull(debut, "debut ne doit pas être null");
        Objects.requireNonNull(fin, "fin ne doit pas être null");
        if (fin.isBefore(debut)) {
            throw new IllegalArgumentException("dateFin doit être après dateDebut");
        }
        if (dureeMinutes <= 0) {
            throw new IllegalArgumentException("dureeMinutes doit être > 0");
        }

        this.dateDebut = debut;
        this.dateFin = fin;
        this.dureeMinutes = dureeMinutes;
    }

    public boolean estDisponible(LocalDateTime now) {
        if (now == null || dateDebut == null || dateFin == null) {
            return false;
        }
        return !now.isBefore(dateDebut) && !now.isAfter(dateFin);
    }

    public boolean estOuvert(LocalDateTime now) {
        return etat == EtatExamen.OUVERT && estDisponible(now);
    }

    public void ouvrir() {
        this.etat = EtatExamen.OUVERT;
    }

    public void fermer() {
        this.etat = EtatExamen.FERME;
    }

    public void ajouterQuestion(Question question) {
        Objects.requireNonNull(question, "question ne doit pas être null");
        this.questions.add(question);
    }

    public void retirerQuestion(UUID questionId) {
        if (questionId == null) return;
        questions.removeIf(q -> questionId.equals(q.getId()));
    }

    public double totalPoints() {
        return questions.stream()
                .mapToDouble(Question::getBareme)
                .sum();
    }

    public Question trouverQuestion(UUID questionId) {
        if (questionId == null) return null;
        return questions.stream()
                .filter(q -> questionId.equals(q.getId()))
                .findFirst()
                .orElse(null);
    }

    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public List<Inscription> getInscriptions() {
        return Collections.unmodifiableList(inscriptions);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = (id != null) ? id : UUID.randomUUID();
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public int getDureeMinutes() {
        return dureeMinutes;
    }

    public void setDureeMinutes(int dureeMinutes) {
        this.dureeMinutes = dureeMinutes;
    }

    public EtatExamen getEtat() {
        return etat;
    }

    public void setEtat(EtatExamen etat) {
        this.etat = etat;
    }

    public UUID getCreateurId() {
        return createurId;
    }

    public void setCreateurId(UUID createurId) {
        this.createurId = createurId;
    }

    public NotePublication getPublication() {
        return publication;
    }

    public void setPublication(NotePublication publication) {
        this.publication = publication;
    }
}
