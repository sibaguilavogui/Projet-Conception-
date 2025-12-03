package ca.uqac.examgu.model;

import ca.uqac.examgu.model.Enumerations.PolitiqueCorrectionQCM;
import jakarta.persistence.*;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "questions_choix")
@PrimaryKeyJoinColumn(name = "question_id")
public class QuestionAChoix extends Question {

    public enum TypeChoix {
        QCM,
        UNIQUE
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "type_choix", nullable = false)
    private TypeChoix typeChoix;

    @Enumerated(EnumType.STRING)
    @Column(name = "politique_correction_qcm", nullable = false)
    private PolitiqueCorrectionQCM politiqueCorrectionQCM = PolitiqueCorrectionQCM.TOUT_OU_RIEN;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_choix_id")
    private List<ReponsePossible> reponsesPossibles = new ArrayList<>();

    @Column(name = "nombre_choix_min")
    private int nombreChoixMin = 1;

    @Column(name = "nombre_choix_max")
    private int nombreChoixMax = 1;

    public QuestionAChoix() {
        super();
    }

    public QuestionAChoix(String enonce, double bareme, TypeChoix typeChoix,
                          int nombreChoixMin, int nombreChoixMax,
                          PolitiqueCorrectionQCM politiqueCorrectionQCM, Examen examen) {
        super(enonce, bareme, examen);
        this.typeChoix = typeChoix;
        this.nombreChoixMin = nombreChoixMin;
        this.nombreChoixMax = nombreChoixMax;
        this.politiqueCorrectionQCM = politiqueCorrectionQCM;
    }

    public void ajouterReponsePossible(ReponsePossible reponse) {
        Objects.requireNonNull(reponse, "La réponse ne peut pas être null");
        reponsesPossibles.add(reponse);
    }

    public void ajouterReponsePossible(String libelle, boolean correcte) {
        ReponsePossible reponse = new ReponsePossible(libelle, correcte);
        ajouterReponsePossible(reponse);
    }

    public void retirerReponsePossible(UUID reponseId) {
        if (reponseId == null) return;

        reponsesPossibles.removeIf(r -> reponseId.equals(r.getId()));
    }

    public void definirCommeCorrecte(UUID reponseId) {
        reponsesPossibles.forEach(ReponsePossible::marquerIncorrecte);

        reponsesPossibles.stream()
                .filter(r -> reponseId.equals(r.getId()))
                .findFirst()
                .ifPresent(ReponsePossible::marquerCorrecte);
    }

    public void definirPlusieursCorrectes(List<UUID> reponsesCorrectes) {
        if (nombreChoixMax == 1) {
            throw new IllegalStateException("Impossible d'avoir plusieurs réponses correctes pour une question à choix unique");
        }

        reponsesPossibles.forEach(ReponsePossible::marquerIncorrecte);

        reponsesPossibles.stream()
                .filter(r -> reponsesCorrectes.contains(r.getId()))
                .forEach(ReponsePossible::marquerCorrecte);
    }

    public double calculerNote(ReponseDonnee reponseDonnee) {
        if (reponseDonnee == null || reponseDonnee.getContenu() == null) {
            return 0.0;
        }

        if (typeChoix == TypeChoix.UNIQUE) {
            return calculerNoteChoixUnique(reponseDonnee);
        } else {
            return calculerNoteQCM(reponseDonnee);
        }
    }

    private double calculerNoteChoixUnique(ReponseDonnee reponseDonnee) {
        try {
            UUID reponseId = UUID.fromString(reponseDonnee.getContenu());
            ReponsePossible reponseChoisie = reponsesPossibles.stream()
                    .filter(r -> r.getId().equals(reponseId))
                    .findFirst()
                    .orElse(null);

            if (reponseChoisie != null && reponseChoisie.isCorrecte()) {
                return getBareme();
            }
        } catch (IllegalArgumentException e) {
           return 0;
        }
        return 0.0;
    }

    private double calculerNoteQCM(ReponseDonnee reponseDonnee) {
        try {
            String[] idsSelectionnes = reponseDonnee.getContenu().split(",");
            Set<UUID> reponsesSelectionnees = Arrays.stream(idsSelectionnes)
                    .map(UUID::fromString)
                    .collect(Collectors.toSet());

            long bonnesReponsesSelectionnees = reponsesPossibles.stream()
                    .filter(r -> r.isCorrecte() && reponsesSelectionnees.contains(r.getId()))
                    .count();

            long mauvaisesReponsesSelectionnees = reponsesSelectionnees.size() - bonnesReponsesSelectionnees;
            long totalBonnesReponses = getNombreReponsesCorrectes();

            if (totalBonnesReponses == 0) {
                return 0.0;
            }

            switch (politiqueCorrectionQCM) {
                case TOUT_OU_RIEN:
                    return calculerNoteToutOuRien(bonnesReponsesSelectionnees, mauvaisesReponsesSelectionnees,
                            totalBonnesReponses);
                case MOYENNE_BONS:
                    return calculerNoteMoyenneBons(bonnesReponsesSelectionnees, totalBonnesReponses);
                case ANNULATION:
                    return calculerNoteAnnulation(bonnesReponsesSelectionnees, mauvaisesReponsesSelectionnees,
                            totalBonnesReponses);
                default:
                    return 0.0;
            }
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double calculerNoteToutOuRien(long bonnesSelectionnees, long mauvaisesSelectionnees,
                                          long totalBonnes) {
        if (bonnesSelectionnees == totalBonnes && mauvaisesSelectionnees == 0) {
            return getBareme();
        }
        return 0.0;
    }

    private double calculerNoteMoyenneBons(long bonnesSelectionnees, long totalBonnes) {
        double ratio = (double) bonnesSelectionnees / totalBonnes;
        return ratio * getBareme();
    }

    private double calculerNoteAnnulation(long bonnesSelectionnees, long mauvaisesSelectionnees,
                                          long totalBonnes) {
        long scoreNet = bonnesSelectionnees - mauvaisesSelectionnees;
        if (scoreNet < 0) {
            scoreNet = 0;
        }
        double ratio = (double) scoreNet / totalBonnes;
        return ratio * getBareme();
    }

    public PolitiqueCorrectionQCM getPolitiqueCorrectionQCM() {
        return politiqueCorrectionQCM;
    }

    public void setPolitiqueCorrectionQCM(PolitiqueCorrectionQCM politiqueCorrectionQCM) {
        this.politiqueCorrectionQCM = politiqueCorrectionQCM;
    }

    @Override
    public boolean estAutoCorrectible() {
        return true;
    }

    public TypeChoix getTypeChoix() {
        return typeChoix;
    }

    public void setTypeChoix(TypeChoix typeChoix) {
        this.typeChoix = Objects.requireNonNull(typeChoix, "Le type de choix ne peut pas être null");
    }

    public List<ReponsePossible> getReponsesPossibles() {
        return Collections.unmodifiableList(reponsesPossibles);
    }

    public void setReponsesPossibles(List<ReponsePossible> reponsesPossibles) {
        this.reponsesPossibles.clear();
        if (reponsesPossibles != null) {
            this.reponsesPossibles.addAll(reponsesPossibles);
        }
    }

    public int getNombreChoixMin() {
        return nombreChoixMin;
    }

    public void setNombreChoixMin(int nombreChoixMin) {
        if (nombreChoixMin < 0 || nombreChoixMin > nombreChoixMax) {
            throw new IllegalArgumentException("Le nombre minimum de choix est invalide");
        }
        this.nombreChoixMin = nombreChoixMin;
    }

    public int getNombreChoixMax() {
        return nombreChoixMax;
    }

    public void setNombreChoixMax(int nombreChoixMax) {
        if (nombreChoixMax < nombreChoixMin) {
            throw new IllegalArgumentException("Le nombre maximum de choix doit être >= au nombre minimum");
        }
        this.nombreChoixMax = nombreChoixMax;
    }

    public int getNombreReponsesCorrectes() {
        return (int) reponsesPossibles.stream()
                .filter(ReponsePossible::isCorrecte)
                .count();
    }

    @Override
    public String toString() {
        return String.format("QuestionAChoix{id=%s, enonce='%s', type=%s, bareme=%.1f}",
                getId(), getEnonce(), typeChoix, getBareme());
    }
}