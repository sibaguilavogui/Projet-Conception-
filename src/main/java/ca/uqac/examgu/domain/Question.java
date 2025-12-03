package ca.uqac.examgu.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Question {

    private UUID id;
    private String enonce;
    private TypeQuestion type;
    private double bareme;
    private final List<ReponsePossible> reponsesPossibles;

    public Question(UUID id, String enonce, TypeQuestion type, double bareme) {
        this.id = (id != null) ? id : UUID.randomUUID();
        this.enonce = enonce;
        this.type = type;
        this.bareme = bareme;
        this.reponsesPossibles = new ArrayList<>();
    }

    public Question(String enonce, TypeQuestion type, double bareme) {
        this(UUID.randomUUID(), enonce, type, bareme);
    }

    // ===================== FACTORIES (pour ton Main) =====================

    /** QCM auto: 1 bonne + 2 mauvaises */
    public static Question creerQcmAuto(String enonce, double bareme,
                                        String bonne, String mauvaise1, String mauvaise2) {
        Question q = new Question(enonce, TypeQuestion.QCM, bareme);

        // ⚠️ SI TON CONSTRUCTEUR ReponsePossible(...) est différent,
        // remplace les 3 lignes new ReponsePossible(...) par ton constructeur à toi.
        q.ajouterReponsePossible(new ReponsePossible(bonne, true));
        q.ajouterReponsePossible(new ReponsePossible(mauvaise1, false));
        q.ajouterReponsePossible(new ReponsePossible(mauvaise2, false));

        return q;
    }

    /** Question courte: 1 réponse correcte attendue */
    public static Question creerCourte(String enonce, double bareme, String reponseAttendue) {
        Question q = new Question(enonce, TypeQuestion.COURTE, bareme);
        q.ajouterReponsePossible(new ReponsePossible(reponseAttendue, true));
        return q;
    }

    /** Vrai/Faux: correct=true => "vrai" est correct, sinon "faux" est correct */
    public static Question creerVraiFaux(String enonce, double bareme, boolean correct) {
        Question q = new Question(enonce, TypeQuestion.VRAI_FAUX, bareme);
        q.ajouterReponsePossible(new ReponsePossible("vrai", correct));
        q.ajouterReponsePossible(new ReponsePossible("faux", !correct));
        return q;
    }

    // ====================================================================

    // UML: ajouterReponsePossible(rep: ReponsePossible): void
    public void ajouterReponsePossible(ReponsePossible rep) {
        Objects.requireNonNull(rep, "rep ne doit pas être null");
        this.reponsesPossibles.add(rep);
    }

    // UML: retirerReponsePossible(repId: UUID): void
    public void retirerReponsePossible(UUID repId) {
        if (repId == null) return;
        reponsesPossibles.removeIf(r -> repId.equals(r.getId()));
    }

    // UML: estValide(): boolean
    public boolean estValide() {
        if (enonce == null || enonce.isBlank()) return false;
        if (type == null) return false;
        if (bareme <= 0) return false;

        // ✅ COURTE = correction manuelle => pas besoin de reponsesPossibles
        if (type == TypeQuestion.COURTE) return true;

        // ✅ QCM / VRAI_FAUX => il faut des choix
        return !reponsesPossibles.isEmpty();
    }

    public double corriger(String contenu) {
        if (type == TypeQuestion.COURTE) {
            // ✅ pas de correction automatique pour COURTE
            return 0.0;
        }

        if (contenu == null) return 0.0;
        String c = contenu.trim();

        // QCM / VRAI_FAUX
        for (ReponsePossible r : reponsesPossibles) {
            if (r.getLibelle() == null) continue;
            if (c.equals(r.getLibelle().trim())) {
                return r.isCorrecte() ? bareme : 0.0;
            }
        }
        return 0.0;
    }



    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = (id != null) ? id : UUID.randomUUID(); }

    public String getEnonce() { return enonce; }
    public void setEnonce(String enonce) { this.enonce = enonce; }

    public TypeQuestion getType() { return type; }
    public void setType(TypeQuestion type) { this.type = type; }

    public double getBareme() { return bareme; }
    public void setBareme(double bareme) { this.bareme = bareme; }

    public List<ReponsePossible> getReponsesPossibles() {
        return Collections.unmodifiableList(reponsesPossibles);
    }
}
