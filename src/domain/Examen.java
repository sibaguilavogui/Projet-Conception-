package domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class Examen {

    private UUID id;
    private String code; // EX-0001
    private String titre;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private int dureeMinutes;
    private EtatExamen etat;
    private UUID createurId;

    private final List<Question> questions;
    private final List<Inscription> inscriptions;
    private NotePublication publication;

    public Examen(UUID id,
                  String titre,
                  LocalDateTime dateDebut,
                  LocalDateTime dateFin,
                  int dureeMinutes,
                  EtatExamen etat,
                  UUID createurId) {

        this.id = (id != null) ? id : UUID.randomUUID();
        this.code = null; // généré par ExamGuSystem
        this.titre = titre;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.dureeMinutes = dureeMinutes;
        this.etat = (etat != null) ? etat : EtatExamen.BROUILLON;
        this.createurId = createurId;

        this.questions = new ArrayList<>();
        this.inscriptions = new ArrayList<>();
        this.publication = new NotePublication();
    }

    public Examen(String titre, UUID createurId) {
        this(UUID.randomUUID(), titre, null, null, 0, EtatExamen.BROUILLON, createurId);
    }

    public boolean estDisponible(LocalDateTime now) {
        if (now == null || dateDebut == null || dateFin == null) return false;
        return !now.isBefore(dateDebut) && !now.isAfter(dateFin);
    }

    public boolean estOuvert(LocalDateTime now) {
        return etat == EtatExamen.OUVERT && estDisponible(now);
    }

    public void planifier(LocalDateTime debut, LocalDateTime fin, int duree) {
        Objects.requireNonNull(debut, "debut ne doit pas être null");
        Objects.requireNonNull(fin, "fin ne doit pas être null");
        if (fin.isBefore(debut)) throw new IllegalArgumentException("dateFin doit être après dateDebut");
        if (duree <= 0) throw new IllegalArgumentException("dureeMinutes doit être > 0");

        long minutes = Duration.between(debut, fin).toMinutes();
        if (minutes < duree) throw new IllegalArgumentException("Fenêtre (debut->fin) plus petite que dureeMinutes");

        this.dateDebut = debut;
        this.dateFin = fin;
        this.dureeMinutes = duree;
    }

    public void ouvrir() { this.etat = EtatExamen.OUVERT; }
    public void fermer() { this.etat = EtatExamen.FERME; }

    public void ajouterQuestion(Question q) {
        Objects.requireNonNull(q, "q ne doit pas être null");
        this.questions.add(q);
    }

    public void retirerQuestion(UUID qId) {
        if (qId == null) return;
        questions.removeIf(q -> qId.equals(q.getId()));
    }

    public double totalPoints() {
        return questions.stream().mapToDouble(Question::getBareme).sum();
    }

    public Question trouverQuestion(UUID qId) {
        if (qId == null) return null;
        return questions.stream().filter(q -> qId.equals(q.getId())).findFirst().orElse(null);
    }

    public Inscription inscrire(Etudiant etudiant) {
        Objects.requireNonNull(etudiant, "etudiant ne doit pas être null");
        for (Inscription i : inscriptions) {
            if (i.getEtudiant() != null && etudiant.getId().equals(i.getEtudiant().getId())) return i;
        }
        Inscription ins = new Inscription(etudiant, this);
        inscriptions.add(ins);
        return ins;
    }

    public Inscription trouverInscription(UUID etudiantId) {
        if (etudiantId == null) return null;
        return inscriptions.stream()
                .filter(i -> i.getEtudiant() != null && etudiantId.equals(i.getEtudiant().getId()))
                .findFirst().orElse(null);
    }

    public boolean supprimerInscription(UUID etudiantId) {
        if (etudiantId == null) return false;
        return inscriptions.removeIf(i -> i.getEtudiant() != null && etudiantId.equals(i.getEtudiant().getId()));
    }

    public List<Question> getQuestions() { return Collections.unmodifiableList(questions); }
    public List<Inscription> getInscriptions() { return Collections.unmodifiableList(inscriptions); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = (id != null) ? id : UUID.randomUUID(); }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = (code == null || code.isBlank()) ? null : code.trim().toUpperCase(); }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public int getDureeMinutes() { return dureeMinutes; }
    public void setDureeMinutes(int dureeMinutes) { this.dureeMinutes = dureeMinutes; }

    public EtatExamen getEtat() { return etat; }
    public void setEtat(EtatExamen etat) { this.etat = etat; }

    public UUID getCreateurId() { return createurId; }
    public void setCreateurId(UUID createurId) { this.createurId = createurId; }

    public NotePublication getPublication() { return publication; }
    public void setPublication(NotePublication publication) { this.publication = publication; }
}
