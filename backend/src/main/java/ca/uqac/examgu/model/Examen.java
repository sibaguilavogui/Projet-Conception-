package ca.uqac.examgu.model;

import ca.uqac.examgu.model.Enumerations.EtatExamen;
import ca.uqac.examgu.model.Enumerations.PolitiqueCorrectionQCM;
import ca.uqac.examgu.model.Enumerations.StatutInscription;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @ManyToOne
    @JoinColumn(name = "createur_id", nullable = false)
    @JsonIgnore
    private Enseignant createur;

    @OneToMany(mappedBy = "examen", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private final List<Question> questions = new ArrayList<>();

    @OneToMany(mappedBy = "examen", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private final List<Inscription> inscriptions = new ArrayList<>();

    @Column(name = "est_corrige")
    private boolean estCorrige = false;

    @Column(name = "notes_visibles")
    private boolean notesVisibles = false;

    @Column(name = "date_publication_notes")
    private LocalDateTime datePublicationNotes;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    private String description;

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
        System.out.println(1);
        LocalDateTime now = LocalDateTime.now();
        if (etat != EtatExamen.OUVERT) {
            System.out.println(2);
            return false;
        }

        if (dateDebut == null || dateFin == null) {
            System.out.println(3);
            return false;
        }

        System.out.println(4);
        System.out.println(dateDebut);
        System.out.println(dateFin);
        System.out.println(now);
        return !now.isBefore(dateDebut) && !now.isAfter(dateFin);
    }

    public List<String> getValidationsPourEtatPret(){
        List<String> erreurs = new ArrayList<>();

        if (this.etat != EtatExamen.BROUILLON) {
            erreurs.add("Seuls les examens en brouillon peuvent être marqués comme PRÊT");
            return erreurs;
        }

        if (titre == null || titre.trim().isEmpty()) {
            erreurs.add("Le titre de l'examen est obligatoire");
        }

        if (dateDebut == null) {
            erreurs.add("La date de début n'est pas définie");
        }

        if (dateFin == null) {
            erreurs.add("La date de fin n'est pas définie");
        }

        if (dureeMinutes <= 0) {
            erreurs.add("La durée de l'examen doit être positive");
        }

        if (dateDebut != null && dateFin != null) {
            if (dateFin.isBefore(dateDebut)) {
                erreurs.add("La date de fin doit être après la date de début");
            }

            long fenetreMinutes = Duration.between(dateDebut, dateFin).toMinutes();
            if (dureeMinutes > fenetreMinutes) {
                erreurs.add(String.format(
                        "La durée de l'examen (%d minutes) dépasse la fenêtre temporelle (%d minutes)",
                        dureeMinutes, fenetreMinutes
                ));
            }

            if (dateFin.isBefore(LocalDateTime.now())) {
                erreurs.add("La date de fin de l'examen est déjà passée");
            }
        }

        if (questions.isEmpty()) {
            erreurs.add("L'examen doit contenir au moins une question");
        }

        double total = totalPoints();
        if (total <= 0) {
            erreurs.add(String.format("Le barème total doit être positif (actuel: %.2f)", total));
        }

        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);

            if (question.getEnonce() == null || question.getEnonce().trim().isEmpty()) {
                erreurs.add(String.format("Question %d: L'énoncé est vide", i + 1));
                continue;
            }

            if (question.getBareme() < 0) {
                erreurs.add(String.format("Question %d: Le barème doit être positif (%.2f)", i + 1, question.getBareme()));
            }

            if (question instanceof QuestionAChoix) {
                QuestionAChoix qChoix = (QuestionAChoix) question;
                List<ReponsePossible> choix = qChoix.getReponsesPossibles();

                if (choix == null || choix.size()<2) {
                    erreurs.add(String.format("Question %d (choix): Aucun choix n'est défini", i + 1));
                } else {
                    long nbBonnesReponses = choix.stream()
                            .filter(ReponsePossible::isCorrecte)
                            .count();

                    if (nbBonnesReponses == 0) {
                        erreurs.add(String.format("Question %d (choix): Aucune réponse correcte n'est définie", i + 1));
                    }

                    if (qChoix.getTypeChoix() == QuestionAChoix.TypeChoix.UNIQUE && nbBonnesReponses != 1) {
                        erreurs.add(String.format(
                                "Question %d (choix unique): Doit avoir exactement une réponse correcte (actuel: %d)",
                                i + 1, nbBonnesReponses
                        ));
                    }

                }

                if (qChoix.getTypeChoix() == QuestionAChoix.TypeChoix.QCM) {
                    if (qChoix.getPolitiqueCorrectionQCM() == null) {
                        erreurs.add(String.format("Question %d (QCM): La politique de correction doit être définie", i + 1));
                    }

                    if (qChoix.getPolitiqueCorrectionQCM() == PolitiqueCorrectionQCM.MOYENNE_BONNES_ET_MAUVAISES) {
                        long nbBonnesReponses = choix.stream()
                                .filter(ReponsePossible::isCorrecte)
                                .count();
                        if (nbBonnesReponses < 2) {
                            erreurs.add(String.format(
                                    "Question %d (QCM avec politique ANNULATION): Doit avoir au moins 2 réponses correctes",
                                    i + 1
                            ));
                        }
                    }
                }
            }
        }

        return erreurs;
    }

    public void mettreEnEtatPret() {
        List<String> validations = getValidationsPourEtatPret();
        if (!validations.isEmpty()) {
            StringBuilder message = new StringBuilder("Impossible de marquer l'examen comme PRÊT:\n");
            for (String validation : validations) {
                message.append("• ").append(validation).append("\n");
            }
            throw new IllegalStateException(message.toString());
        }

        this.etat = EtatExamen.PRET;
    }

    public void ouvrir() {
        if(getEtat()!=EtatExamen.PRET){
            throw new IllegalStateException("L'examen n'est pas prêt à être ouvert");
        }
        setEtat(EtatExamen.OUVERT);
    }

    public void fermer() {
        this.etat = EtatExamen.FERME;
    }

    public void mettreEnBrouillon() {
        if (this.etat == EtatExamen.OUVERT) {
            throw new IllegalStateException("Impossible de remettre un examen ouvert en brouillon");
        }
        this.etat = EtatExamen.BROUILLON;
    }

    public QuestionAChoix ajouterQuestionAChoix(String enonce, double bareme, QuestionAChoix.TypeChoix typeChoix,
                                                int nombreChoixMin, int nombreChoixMax,
                                                PolitiqueCorrectionQCM politiqueCorrection, List<ReponsePossible> options) {
        QuestionAChoix question = new QuestionAChoix(enonce, bareme, typeChoix,
                nombreChoixMin, nombreChoixMax, politiqueCorrection, this);

        if (etat != EtatExamen.BROUILLON) {
            throw new IllegalStateException("Impossible d'ajouter des questions à un examen non brouillon");
        }
        if (options != null) {
            for (ReponsePossible option : options) {
                question.ajouterReponsePossible(option);
            }
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

    public boolean isEstCorrige() {
        return estCorrige;
    }

    public void setEstCorrige(boolean estCorrige) {
        this.estCorrige = estCorrige;
    }

    public void publierNotes() {
        this.notesVisibles = true;
        this.datePublicationNotes = LocalDateTime.now();
    }

    public void masquerNotes() {
        this.notesVisibles = false;
        this.datePublicationNotes = null;
    }

    public boolean isNotesVisibles() {
        return notesVisibles;
    }

    public void setNotesVisibles(boolean notesVisibles) {
        this.notesVisibles = notesVisibles;
        if (notesVisibles && datePublicationNotes == null) {
            this.datePublicationNotes = LocalDateTime.now();
        }
    }

    public LocalDateTime getDatePublicationNotes() {
        return datePublicationNotes;
    }

    public void setDatePublicationNotes(LocalDateTime datePublicationNotes) {
        this.datePublicationNotes = datePublicationNotes;
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

    @JsonIgnore
    public Enseignant getCreateur() {
        return createur;
    }

    public void setCreateur(Enseignant createur) {
        this.createur = createur;
    }

    @JsonIgnore
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

    @JsonIgnore
    public List<Inscription> getInscriptions() {
        return Collections.unmodifiableList(inscriptions);
    }

    public void setInscriptions(List<Inscription> inscriptions) {
        this.inscriptions.clear();
        if (inscriptions != null) {
            this.inscriptions.addAll(inscriptions);
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