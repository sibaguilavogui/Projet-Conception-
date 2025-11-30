package ca.uqac.examgu.model;

import ca.uqac.examgu.model.Enumerations.EtatExamen;
import ca.uqac.examgu.model.Enumerations.StatutInscription;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "examens")
public class Examen {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String titre;

    @Column(name = "date_debut")
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Column(name = "duree_minutes")
    private int dureeMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EtatExamen etat = EtatExamen.BROUILLON;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createur_id", nullable = false)
    private Enseignant createur;

    @OneToMany(mappedBy = "examen", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Question> questions = new ArrayList<>();

    @OneToMany(mappedBy = "examen", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Inscription> inscriptions = new ArrayList<>();

    @OneToOne(mappedBy = "examen", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private NotePublication publication;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    private String description;

    private boolean notationAutomatique = false;

    public Examen() {}

    public Examen(String titre, Enseignant createur) {
        this.titre = Objects.requireNonNull(titre, "Le titre ne peut pas être null");
        this.createur = Objects.requireNonNull(createur, "Le créateur ne peut pas être null");
    }

    public Examen(String titre, String description, LocalDateTime dateDebut, LocalDateTime dateFin,
                  int dureeMinutes, Enseignant createur) {
        this.titre = Objects.requireNonNull(titre, "Le titre ne peut pas être null");
        this.createur = Objects.requireNonNull(createur, "Le créateur ne peut pas être null");
        this.description=description;
        this.dateDebut=dateDebut;
        this.dateFin=dateFin;
        this.dureeMinutes=dureeMinutes;
    }

    public void planifier(LocalDateTime debut, LocalDateTime fin, int dureeMinutes) {
        Objects.requireNonNull(debut, "La date de début ne peut pas être null");
        Objects.requireNonNull(fin, "La date de fin ne peut pas être null");

        if (fin.isBefore(debut)) {
            throw new IllegalArgumentException("La date de fin doit être après la date de début");
        }
        if (dureeMinutes <= 0) {
            throw new IllegalArgumentException("La durée doit être positive");
        }

        long fenetreMinutes = Duration.between(debut, fin).toMinutes();
        if (dureeMinutes > fenetreMinutes) {
            throw new IllegalArgumentException("La durée de l'examen ne peut pas dépasser la fenêtre temporelle");
        }

        this.dateDebut = debut;
        this.dateFin = fin;
        this.dureeMinutes = dureeMinutes;
    }

    public boolean estDisponible() {
        return estDisponible(LocalDateTime.now());
    }

    public boolean estDisponible(LocalDateTime now) {
        if (now == null || dateDebut == null || dateFin == null) {
            return false;
        }
        return !now.isBefore(dateDebut) && !now.isAfter(dateFin);
    }

    public boolean estOuvert() {
        return estOuvert(LocalDateTime.now());
    }

    public boolean estOuvert(LocalDateTime now) {
        return etat == EtatExamen.OUVERT && estDisponible(now);
    }

    public void ouvrir() {
        if (this.etat != EtatExamen.BROUILLON) {
            throw new IllegalStateException("Seuls les examens en brouillon peuvent être ouverts");
        }

        if (dateDebut == null || dateFin == null || dureeMinutes <= 0) {
            throw new IllegalStateException("L'examen doit être planifié avant d'être ouvert");
        }
        if (questions.isEmpty()) {
            throw new IllegalStateException("L'examen doit contenir au moins une question");
        }
        if (totalPoints() <= 0) {
            throw new IllegalStateException("L'examen doit avoir un barème total positif");
        }

        this.etat = EtatExamen.OUVERT;
    }

    public void fermer() {
        this.etat = EtatExamen.FERME;

        if (notationAutomatique) {
            //corrigerAutomatiquement();
        }
    }

    public void mettreEnBrouillon() {
        if (this.etat == EtatExamen.OUVERT) {
            throw new IllegalStateException("Impossible de remettre un examen ouvert en brouillon");
        }
        this.etat = EtatExamen.BROUILLON;
    }

    public QuestionAChoix ajouterQuestionAChoix(String enonce, double bareme, QuestionAChoix.TypeChoix typeChoix,
                                int nombreChoixMin,
                                int nombreChoixMax) {
        QuestionAChoix question = new QuestionAChoix(enonce, bareme, typeChoix,
                nombreChoixMin, nombreChoixMax, this);

        if (etat != EtatExamen.BROUILLON) {
            throw new IllegalStateException("Impossible d'ajouter des questions à un examen non brouillon");
        }

        this.questions.add(question);
        return question;
    }

    public QuestionADeveloppement ajouterQuestionADeveloppement(String enonce, double bareme) {
        QuestionADeveloppement question = new QuestionADeveloppement(enonce, bareme, this);

        if (etat != EtatExamen.BROUILLON) {
            throw new IllegalStateException("Impossible d'ajouter des questions à un examen non brouillon");
        }

        this.questions.add(question);
        return question;
    }

    public void retirerQuestion(UUID questionId) {
        if (questionId == null) return;

        if (etat != EtatExamen.BROUILLON) {
            throw new IllegalStateException("Impossible de retirer des questions d'un examen non brouillon");
        }

        questions.removeIf(q -> questionId.equals(q.getId()));
    }

    public Question trouverQuestion(UUID questionId) {
        if (questionId == null) return null;
        return questions.stream()
                .filter(q -> questionId.equals(q.getId()))
                .findFirst()
                .orElse(null);
    }

    public Inscription inscrireEtudiant(Etudiant etudiant) {
        Objects.requireNonNull(etudiant, "L'étudiant ne peut pas être null");

        Optional<Inscription> inscriptionExistante = inscriptions.stream()
                .filter(i -> i.getEtudiant().equals(etudiant) && i.estActive())
                .findFirst();

        if (inscriptionExistante.isPresent()) {
            if (inscriptionExistante.get().estActive()) {
                throw new RuntimeException("L'étudiant est déjà inscrit à cet examen");
            } else {
                inscriptionExistante.get().activer();
                return inscriptionExistante.get();
            }
        }

        Inscription inscription = new Inscription(StatutInscription.ACTIVE, etudiant, this);
        inscriptions.add(inscription);
        return inscription;
    }

    public boolean estInscrit(Etudiant etudiant) {
        return inscriptions.stream()
                .anyMatch(i -> i.getEtudiant().equals(etudiant) && i.estActive());
    }

    public double totalPoints() {
        return questions.stream()
                .mapToDouble(Question::getBareme)
                .sum();
    }

    public boolean peutDemarrerTentative(Etudiant etudiant) {
        return estOuvert() && estInscrit(etudiant);
    }

    public long getTempsRestant(LocalDateTime maintenant) {
        if (dateFin == null || maintenant.isAfter(dateFin)) {
            return 0;
        }
        return Duration.between(maintenant, dateFin).toMinutes();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = Objects.requireNonNull(titre, "Le titre ne peut pas être null");
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
        if (dureeMinutes <= 0) {
            throw new IllegalArgumentException("La durée doit être positive");
        }
        this.dureeMinutes = dureeMinutes;
    }

    public EtatExamen getEtat() {
        return etat;
    }

    public void setEtat(EtatExamen etat) {
        this.etat = etat;
    }

    public Enseignant getCreateur() {
        return createur;
    }

    public void setCreateur(Enseignant createur) {
        this.createur = Objects.requireNonNull(createur, "Le créateur ne peut pas être null");
    }

    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public void setQuestions(List<Question> questions) {
        this.questions.clear();
        if (questions != null) {
            this.questions.addAll(questions);
            this.questions.forEach(q -> q.setExamen(this));
        }
    }

    public List<Inscription> getInscriptions() {
        return Collections.unmodifiableList(inscriptions);
    }

    public void setInscriptions(List<Inscription> inscriptions) {
        this.inscriptions.clear();
        if (inscriptions != null) {
            this.inscriptions.addAll(inscriptions);
        }
    }

    public NotePublication getPublication() {
        return publication;
    }

    public void setPublication(NotePublication publication) {
        this.publication = publication;
        if (publication != null) {
            publication.setExamen(this);
        }
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isNotationAutomatique() {
        return notationAutomatique;
    }

    public void setNotationAutomatique(boolean notationAutomatique) {
        this.notationAutomatique = notationAutomatique;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Examen)) return false;
        Examen examen = (Examen) o;
        return Objects.equals(id, examen.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Examen{id=%s, titre='%s', etat=%s, createur=%s}",
                id, titre, etat, createur != null ? createur.getEmail() : "null");
    }
}