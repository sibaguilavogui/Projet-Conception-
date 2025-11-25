package domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Représente une question d'examen.
 *
 * Attributs (UML) :
 *  - id : UUID
 *  - enonce : String
 *  - type : TypeQuestion
 *  - bareme : double
 *  - reponsesPossibles : List<ReponsePossible>
 *
 * Méthodes (UML) :
 *  + ajouterReponsePossible(r: ReponsePossible): void
 *  + retirerReponsePossible(repid: UUID): void
 *  + estValide(): boolean
 *  + corriger(contenu: String): double
 */
public class Question {

    private UUID id;
    private String enonce;
    private TypeQuestion type;
    private double bareme;
    private final List<ReponsePossible> reponsesPossibles;

    // ---------- Constructeurs ----------

    public Question() {
        this(UUID.randomUUID(), "", null, 0.0);
    }

    public Question(UUID id, String enonce, TypeQuestion type, double bareme) {
        this.id = (id != null) ? id : UUID.randomUUID();
        this.enonce = enonce;
        this.type = type;
        this.bareme = bareme;
        this.reponsesPossibles = new ArrayList<>();
    }

    // ---------- Méthodes UML ----------

    public void ajouterReponsePossible(ReponsePossible r) {
        Objects.requireNonNull(r, "La réponse possible ne doit pas être null");
        this.reponsesPossibles.add(r);
    }

    public void retirerReponsePossible(UUID repId) {
        if (repId == null) {
            return;
        }
        reponsesPossibles.removeIf(r -> repId.equals(r.getId()));
    }

    /**
     * Vérifie si la question est "valide".
     * Ici on considère :
     *  - énoncé non vide
     *  - barème > 0
     *  - au moins une réponse possible
     *
     * Si plus tard vous voulez ajouter la règle "au moins une réponse correcte",
     * il suffira de modifier cette méthode (modifiabilité).
     */
    public boolean estValide() {
        return enonce != null
                && !enonce.isBlank()
                && bareme > 0
                && !reponsesPossibles.isEmpty();
    }

    /**
     * Corrige une réponse donnée sous forme de texte.
     *
     * Implémentation simple :
     *  - si le contenu correspond à une réponse marquée correcte,
     *    on renvoie le barème complet.
     *  - sinon 0.
     *
     * Si plus tard vous voulez des points partiels ou plusieurs bonnes réponses,
     * il suffira de changer cette logique ici.
     */
    public double corriger(String contenu) {
        if (contenu == null) {
            return 0.0;
        }

        for (ReponsePossible r : reponsesPossibles) {
            if (contenu.equals(r.getLibelle())) {
                return r.isCorrecte() ? bareme : 0.0;
            }
        }
        return 0.0;
    }

    // ---------- Getters / Setters ----------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = (id != null) ? id : UUID.randomUUID();
    }

    public String getEnonce() {
        return enonce;
    }

    public void setEnonce(String enonce) {
        this.enonce = enonce;
    }

    public TypeQuestion getType() {
        return type;
    }

    public void setType(TypeQuestion type) {
        this.type = type;
    }

    public double getBareme() {
        return bareme;
    }

    public void setBareme(double bareme) {
        this.bareme = bareme;
    }

    /**
     * Vue non modifiable de la liste des réponses possibles.
     * Pour ajouter / retirer, utiliser les méthodes prévues.
     */
    public List<ReponsePossible> getReponsesPossibles() {
        return Collections.unmodifiableList(reponsesPossibles);
    }
}
