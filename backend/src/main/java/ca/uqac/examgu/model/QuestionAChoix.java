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

    public double calculerNote(ReponseDonnee reponseDonnee) throws Exception {
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
            UUID reponseId = UUID.fromString(reponseDonnee.getContenu().trim());
            ReponsePossible reponseChoisie = reponsesPossibles.stream()
                    .filter(r -> r.getId().equals(reponseId))
                    .findFirst()
                    .orElse(null);

            if (reponseChoisie != null && reponseChoisie.isCorrecte()) {
                return getBareme();
            }
            return 0.0;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Erreur"+e.getMessage());
        }

    }

    private double calculerNoteQCM(ReponseDonnee reponseDonnee) throws Exception {
        try {
            String contenuBrut = reponseDonnee.getContenu();
            if (contenuBrut == null || contenuBrut.isBlank()) return 0.0;

            contenuBrut = contenuBrut
                    .replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .trim();

            String[] idsSelectionnes = contenuBrut.split(",");
            Set<UUID> reponsesSelectionnees = Arrays.stream(idsSelectionnes)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
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
                case MOYENNE_BONNES:
                    return calculerNoteMoyenneBons(bonnesReponsesSelectionnees, totalBonnesReponses);
                case MOYENNE_BONNES_ET_MAUVAISES:
                    return calculerNoteAnnulation(bonnesReponsesSelectionnees, mauvaisesReponsesSelectionnees,
                            totalBonnesReponses);
                default:
                    return 0.0;
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
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

    public int getNombreReponsesCorrectes() {
        return (int) reponsesPossibles.stream()
                .filter(ReponsePossible::isCorrecte)
                .count();
    }

    @Override
    public String getType() {
        return "CHOIX";
    }

    @Override
    public String toString() {
        return String.format("QuestionAChoix{id=%s, enonce='%s', type=%s, bareme=%.1f}",
                getId(), getEnonce(), typeChoix, getBareme());
    }
}