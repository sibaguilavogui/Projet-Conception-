package ca.uqac.examgu.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "questions_choix")
@PrimaryKeyJoinColumn(name = "question_id")
public class QuestionAChoix extends Question {

    public enum TypeChoix {
        QCM,
        VRAI_FAUX
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "type_choix", nullable = false)
    private TypeChoix typeChoix;

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
                          int nombreChoixMin,
                          int nombreChoixMax, Examen examen) {
        super(enonce, bareme, examen);
        this.typeChoix = typeChoix;
        this.nombreChoixMin = nombreChoixMin;
        this.nombreChoixMax = nombreChoixMax;
    }

    private void ajouterReponseVF() {
        if (typeChoix != TypeChoix.VRAI_FAUX) {
            throw new IllegalStateException("Cette méthode est réservée aux questions Vrai/Faux");
        }

        reponsesPossibles.clear();

        ReponsePossible vrai = new ReponsePossible("Vrai", true);
        ReponsePossible faux = new ReponsePossible("Faux", false);

        reponsesPossibles.add(vrai);
        reponsesPossibles.add(faux);

        this.nombreChoixMin = 1;
        this.nombreChoixMax = 1;
    }

    public void ajouterReponsePossible(ReponsePossible reponse) {
        Objects.requireNonNull(reponse, "La réponse ne peut pas être null");

        if (typeChoix == TypeChoix.VRAI_FAUX && reponsesPossibles.size() >= 2) {
            throw new IllegalStateException("Une question Vrai/Faux ne peut avoir que deux réponses");
        }

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

        if (typeChoix == TypeChoix.VRAI_FAUX || nombreChoixMax == 1) {
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



        return 0.0;
    }

    public List<ReponsePossible> getReponsesPourAffichage() {
        List<ReponsePossible> reponses = new ArrayList<>(reponsesPossibles);

        return Collections.unmodifiableList(reponses);
    }

    @Override
    public boolean estValide() {
        return getReponsesPossibles().size()>1 && !getEnonce().isEmpty() && getBareme()>=0;
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

        if (typeChoix == TypeChoix.VRAI_FAUX) {
            ajouterReponseVF();
        }
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
        if (nombreChoixMax < nombreChoixMin || nombreChoixMax > reponsesPossibles.size()) {
            throw new IllegalArgumentException("Le nombre maximum de choix est invalide");
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