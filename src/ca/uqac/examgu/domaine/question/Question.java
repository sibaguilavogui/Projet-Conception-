package ca.uqac.examgu.domaine.question;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Question du système Exam-GU.
 * Modèle aligné sur le diagramme (id, enonce, type, bareme, reponsesPossibles)
 * + méthodes métier : ajouter/retirer, estValide, corriger.
 */
public class Question {

    private final UUID id;
    private String enonce;
    private TypeQuestion type;
    private double bareme;
    private final List<ReponsePossible> reponsesPossibles = new ArrayList<>();

    public Question(UUID id, String enonce, TypeQuestion type, double bareme) {
        if (id == null) throw new IllegalArgumentException("id requis");
        if (enonce == null || enonce.isBlank()) throw new IllegalArgumentException("énoncé requis");
        if (type == null) throw new IllegalArgumentException("type requis");
        if (bareme < 0.0) throw new IllegalArgumentException("barème >= 0 requis");
        this.id = id;
        this.enonce = enonce;
        this.type = type;
        this.bareme = bareme;
    }

    // ====== Méthodes métier ======

    /** Ajoute une réponse possible (ignore les doublons d'id). */
    public void ajouterReponsePossible(ReponsePossible r) {
        Objects.requireNonNull(r, "réponse possible requise");
        boolean existe = reponsesPossibles.stream().anyMatch(x -> x.getId().equals(r.getId()));
        if (!existe) {
            reponsesPossibles.add(r);
        }
    }

    /** Retire une réponse possible par son id. */
    public void retirerReponsePossible(UUID repId) {
        reponsesPossibles.removeIf(r -> r.getId().equals(repId));
    }

    /**
     * Validité minimale de la question selon le type.
     * - QCM : ≥2 choix et ≥1 correct
     * - VRAI_FAUX : exactement 2 choix dont 1 correct
     * - COURTE : au moins un énoncé non vide (pas d’exigence de réponses possibles)
     */
    public boolean estValide() {
        if (enonce == null || enonce.isBlank()) return false;
        if (bareme < 0.0) return false;

        switch (type) {
            case QCM:
                long nbCorrect = reponsesPossibles.stream().filter(ReponsePossible::isCorrecte).count();
                return reponsesPossibles.size() >= 2 && nbCorrect >= 1;
            case VRAI_FAUX:
                long nb = reponsesPossibles.size();
                long correct = reponsesPossibles.stream().filter(ReponsePossible::isCorrecte).count();
                return nb == 2 && correct == 1;
            case COURTE:
                return true; // correction généralement manuelle
            default:
                return false;
        }
    }

    /**
     * Corrige un contenu textuel de l’étudiant et retourne les points attribués.
     * Règles simples et extensibles :
     * - QCM / VRAI_FAUX : compare au libellé des réponses correctes (sans casse).
     *   On accepte soit le libellé exact, soit une liste d’UUID séparés par des virgules (id des choix sélectionnés).
     * - COURTE : par défaut 0 (correction manuelle); peut être ajusté plus tard (mots-clés, similarité, etc.).
     */
    public double corriger(String contenu) {
        if (bareme <= 0) return 0.0;
        if (contenu == null) contenu = "";

        switch (type) {
            case QCM:
            case VRAI_FAUX: {
                // 1) Correspondance par libellé (texte)
                Set<String> correctLabels = reponsesPossibles.stream()
                        .filter(ReponsePossible::isCorrecte)
                        .map(r -> r.getLibelle().trim().toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet());

                String normalized = contenu.trim().toLowerCase(Locale.ROOT);
                if (correctLabels.contains(normalized)) {
                    return bareme;
                }

                // 2) Option : correspondance par UUID listés "id1,id2"
                try {
                    Set<UUID> choisis = Arrays.stream(contenu.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(UUID::fromString)
                            .collect(Collectors.toSet());

                    Set<UUID> correctIds = reponsesPossibles.stream()
                            .filter(ReponsePossible::isCorrecte)
                            .map(ReponsePossible::getId)
                            .collect(Collectors.toSet());

                    if (!choisis.isEmpty() && choisis.equals(correctIds)) {
                        return bareme;
                    }
                } catch (Exception ignore) {
                    // contenu non parsable en UUID → on ne sanctionne pas, on retombe à 0
                }
                return 0.0;
            }
            case COURTE:
                return 0.0; // correction manuelle par défaut
            default:
                return 0.0;
        }
    }

    // ====== Getters / Setters minimaux ======

    public UUID getId() { return id; }

    public String getEnonce() { return enonce; }
    public void setEnonce(String enonce) {
        if (enonce == null || enonce.isBlank()) throw new IllegalArgumentException("énoncé requis");
        this.enonce = enonce;
    }

    public TypeQuestion getType() { return type; }
    public void setType(TypeQuestion type) { this.type = Objects.requireNonNull(type); }

    public double getBareme() { return bareme; }
    public void setBareme(double bareme) {
        if (bareme < 0.0) throw new IllegalArgumentException("barème >= 0 requis");
        this.bareme = bareme;
    }

    /** Retourne une vue non modifiable des réponses. */
    public List<ReponsePossible> getReponsesPossibles() {
        return Collections.unmodifiableList(reponsesPossibles);
    }

    // equals/hashCode sur l'id (entité)
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Question)) return false;
        Question q = (Question) o;
        return id.equals(q.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        return "Question{" + "id=" + id + ", type=" + type + ", bareme=" + bareme + '}';
    }
}
