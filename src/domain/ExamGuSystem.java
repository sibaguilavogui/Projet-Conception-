package domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExamGuSystem {

    private static final Logger log = Logger.getLogger(ExamGuSystem.class.getName());

    // ✅ Une seule tentative active par (examen + étudiant)
    private final Map<String, UUID> tentativeActiveParCle = new HashMap<>();
    private final Map<UUID, String> cleParTentative = new HashMap<>();

    private static String cleTentative(UUID examenId, UUID etuId) {
        return examenId.toString() + "|" + etuId.toString();
    }

    private final Map<UUID, Utilisateur> utilisateurs = new HashMap<>();
    private final Map<UUID, Examen> examens = new HashMap<>();
    private final Map<UUID, Tentative> tentatives = new HashMap<>();

    // Compteurs pour IDs humains (lisibles)
    private int nextExamNo = 1;
    private int nextEtuNo = 1;
    private int nextEnsNo = 1;
    private int nextAdmNo = 1;

    private static String code(String prefix, int n) {
        return String.format("%s-%04d", prefix, n);
    }

    private void assignCode(Utilisateur u) {
        // 1) Code "lisible" (ETU-0001 / ENS-0001 / ADM-0001)
        if (u.getCode() == null) {
            switch (u.getRole()) {
                case ETUDIANT -> u.setCode(code("ETU", nextEtuNo++));
                case ENSEIGNANT -> u.setCode(code("ENS", nextEnsNo++));
                case ADMIN -> u.setCode(code("ADM", nextAdmNo++));
            }
        }

        // 2) Code permanent (seulement ETU/ENS) si on a les infos
        if (u.getRole() != Role.ADMIN && u.getCodePermanent() == null) {
            String prenom = u.getPrenom();
            String nom = u.getNom();
            LocalDate dn = u.getDateNaissance();
            if (prenom != null && nom != null && dn != null) {
                u.setCodePermanent(codePermanentUnique(prenom, nom, dn));
            }
        }
    }

    private void assignCode(Examen ex) {
        if (ex.getCode() != null) return;
        ex.setCode(code("EX", nextExamNo++));
    }

    // --------- Résolution pratique: "EX-0001", codePermanent, UUID, email ---------

    public Examen trouverExamenParCode(String code) {
        if (code == null) return null;
        String c = code.trim().toUpperCase();
        for (Examen ex : examens.values()) {
            if (c.equals(ex.getCode())) return ex;
        }
        return null;
    }

    public Utilisateur trouverUtilisateurParCode(String code) {
        if (code == null) return null;
        String c = code.trim().toUpperCase();
        for (Utilisateur u : utilisateurs.values()) {
            if (u.getCode() != null && c.equals(u.getCode())) return u;
        }
        return null;
    }

    public Utilisateur trouverUtilisateurParCodePermanent(String codePermanent) {
        if (codePermanent == null) return null;
        String c = codePermanent.trim().toUpperCase();
        for (Utilisateur u : utilisateurs.values()) {
            if (u.getCodePermanent() != null && c.equals(u.getCodePermanent())) return u;
        }
        return null;
    }

    public Examen trouverExamenParRef(String ref) {
        if (ref == null) return null;
        Examen ex = trouverExamenParCode(ref);
        if (ex != null) return ex;
        try { return examens.get(UUID.fromString(ref.trim())); }
        catch (Exception ignored) { return null; }
    }

    public Utilisateur trouverUtilisateurParRef(String ref) {
        if (ref == null) return null;

        Utilisateur u = trouverUtilisateurParCode(ref);
        if (u != null) return u;

        u = trouverUtilisateurParCodePermanent(ref);
        if (u != null) return u;

        try { return utilisateurs.get(UUID.fromString(ref.trim())); }
        catch (Exception ignored) { /* ignore */ }

        // fallback email
        return trouverUtilisateurParEmail(ref.trim());
    }

    public Tentative trouverTentative(UUID id) { return tentatives.get(id); }

    // ✅ utile au prof pour voir les tentatives d’un examen (correction manuelle)
    public List<Tentative> listerTentativesParExamen(UUID examenId) {
        List<Tentative> res = new ArrayList<>();
        for (Tentative t : tentatives.values()) {
            if (t.getExamen() != null && examenId.equals(t.getExamen().getId())) res.add(t);
        }
        return res;
    }

    // ---------------- AUTH ----------------

    public Utilisateur authentifier(String email, String motDePasseEnClair) {
        if (email == null || motDePasseEnClair == null) return null;
        Utilisateur u = trouverUtilisateurParEmail(email.trim());
        if (u != null && u.verifierMotDePasse(motDePasseEnClair)) {
            log.info(() -> String.format("[LOGIN] Auth OK (user=%s/%s, role=%s)", u.getCode(), u.getId(), u.getRole()));
            return u;
        }
        log.warning(() -> "[LOGIN] Auth FAIL (email=" + email + ")");
        return null;
    }

    public Utilisateur trouverUtilisateurParEmail(String email) {
        if (email == null) return null;
        for (Utilisateur u : utilisateurs.values()) {
            if (u.getEmail() != null && u.getEmail().equalsIgnoreCase(email)) return u;
        }
        return null;
    }

    public Collection<Utilisateur> listerUtilisateurs() { return Collections.unmodifiableCollection(utilisateurs.values()); }
    public Collection<Examen> listerExamens() { return Collections.unmodifiableCollection(examens.values()); }
    public Collection<Tentative> listerTentatives() { return Collections.unmodifiableCollection(tentatives.values()); }

    // ---------------- UTILISATEURS (ADMIN) ----------------

    public Utilisateur creerUtilisateur(Utilisateur acteurAdmin, Utilisateur u) {
        requireRole(acteurAdmin, Role.ADMIN);
        Objects.requireNonNull(u, "utilisateur ne doit pas être null");

        // email unique (simple)
        Utilisateur exist = (u.getEmail() == null) ? null : trouverUtilisateurParEmail(u.getEmail());
        if (exist != null) throw new IllegalStateException("Email déjà utilisé: " + u.getEmail());

        assignCode(u);
        utilisateurs.put(u.getId(), u);

        log.info(() -> String.format("[ADMIN] Création utilisateur (admin=%s) user=%s (%s) email=%s",
                acteurAdmin.getCode(), u.getCode(), u.getRole(), u.getEmail()));
        return u;
    }

    public Utilisateur supprimerUtilisateur(Utilisateur acteurAdmin, String refCodeOuCpOuUuidOuEmail) {
        requireRole(acteurAdmin, Role.ADMIN);

        Utilisateur cible = trouverUtilisateurParRef(refCodeOuCpOuUuidOuEmail);
        if (cible == null) throw new IllegalArgumentException("Utilisateur introuvable: " + refCodeOuCpOuUuidOuEmail);

        if (cible.getRole() == Role.ADMIN) {
            throw new IllegalStateException("Suppression interdite: un ADMIN ne peut pas être supprimé.");
        }

        UUID cibleId = cible.getId();

        // 1) Retirer inscriptions de cet étudiant dans tous les examens
        if (cible instanceof Etudiant) {
            for (Examen ex : examens.values()) {
                ex.supprimerInscription(cibleId);
            }
        }

        // 2) Retirer toutes les tentatives de cet étudiant + maps "active"
        if (cible instanceof Etudiant) {
            Iterator<Map.Entry<UUID, Tentative>> it = tentatives.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Tentative> e = it.next();
                Tentative t = e.getValue();
                if (t != null && t.getEtudiant() != null && cibleId.equals(t.getEtudiant().getId())) {
                    UUID tid = e.getKey();

                    String cle = cleParTentative.remove(tid);
                    if (cle != null && tid.equals(tentativeActiveParCle.get(cle))) {
                        tentativeActiveParCle.remove(cle);
                    }
                    it.remove();
                }
            }
        }

        // 3) Supprimer de la map utilisateurs
        utilisateurs.remove(cibleId);

        log.info(() -> String.format("[ADMIN] Suppression utilisateur (admin=%s) user=%s (%s)",
                acteurAdmin.getCode(), cible.getCode(), cible.getRole()));
        return cible;
    }

    // bootstrap (sans contrôle de rôle)
    public Utilisateur creerUtilisateurBootstrap(Utilisateur u) {
        Objects.requireNonNull(u);
        assignCode(u);
        utilisateurs.put(u.getId(), u);
        log.info(() -> String.format("[BOOTSTRAP] user=%s (%s) email=%s",
                u.getCode(), u.getRole(), u.getEmail()));
        return u;
    }

    public Utilisateur creerUtilisateurBootstrapSilencieux(Utilisateur u) {
        Objects.requireNonNull(u);
        assignCode(u);
        utilisateurs.put(u.getId(), u);
        return u;
    }

    // ---------------- EXAMENS (ENSEIGNANT) ----------------

    public Examen creerExamen(Utilisateur acteurEnseignant, String titre) {
        requireRole(acteurEnseignant, Role.ENSEIGNANT);

        Examen ex = new Examen(titre, acteurEnseignant.getId());
        assignCode(ex);
        examens.put(ex.getId(), ex);

        log.info(() -> String.format("[DEMARRER_EXAMEN] Création examen %s (%s) titre=%s par %s",
                ex.getCode(), ex.getId(), titre, acteurEnseignant.getCode()));
        return ex;
    }

    public void planifierEtOuvrirExamen(Utilisateur acteurEnseignant, UUID examenId, LocalDateTime debut, LocalDateTime fin, int dureeMinutes) {
        requireRole(acteurEnseignant, Role.ENSEIGNANT);
        Examen ex = getExamenOrThrow(examenId);

        try {
            ex.planifier(debut, fin, dureeMinutes);
            ex.ouvrir();
            log.info(() -> String.format("[SAUVEGARDE] Planifier+ouvrir examen=%s debut=%s fin=%s duree=%d",
                    ex.getCode(), debut, fin, dureeMinutes));
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Erreur planifierEtOuvrirExamen(examenId=" + examenId + ")", e);
            throw e;
        }
    }

    public void ajouterQuestion(Utilisateur acteurEnseignant, UUID examenId, Question q) {
        requireRole(acteurEnseignant, Role.ENSEIGNANT);
        Examen ex = getExamenOrThrow(examenId);

        Objects.requireNonNull(q, "question ne doit pas être null");
        ex.ajouterQuestion(q);

        log.info(() -> String.format("[SAUVEGARDE] Ajout question examen=%s qId=%s type=%s",
                ex.getCode(), q.getId(), q.getType()));
    }
    public void publierNotes(Utilisateur acteurEnseignant, UUID examenId, LocalDateTime now) {
        requireRole(acteurEnseignant, Role.ENSEIGNANT);
        Examen ex = getExamenOrThrow(examenId);

        int manquantes = compterCorrectionsCourtesManquantes(ex);
        if (manquantes > 0) {
            throw new IllegalStateException("Publication impossible: " + manquantes +
                    " correction(s) manuelle(s) COURTE manquante(s).");
        }

        if (ex.getPublication() == null) ex.setPublication(new NotePublication());
        ex.getPublication().publier(now);

        log.info(() -> String.format("[SAUVEGARDE] Publication notes examen=%s now=%s", ex.getCode(), now));
    }

    private int compterCorrectionsCourtesManquantes(Examen ex) {
        int missing = 0;

        for (Tentative t : tentatives.values()) {
            if (t == null || t.getExamen() == null) continue;
            if (!ex.getId().equals(t.getExamen().getId())) continue;

            for (Question q : ex.getQuestions()) {
                if (q == null || q.getType() != TypeQuestion.COURTE) continue;

                // seulement si l'étudiant a répondu à la question
                String rep = null;
                for (ReponseDonnee rd : t.getReponses()) {
                    if (rd != null && rd.getQuestion() != null && q.getId().equals(rd.getQuestion().getId())) {
                        rep = rd.getContenu();
                        break;
                    }
                }
                if (rep == null || rep.isBlank()) continue;

                // si pas noté manuellement -> manquant
                Double note = t.getNoteManuelle(q.getId());
                if (note == null) missing++;
            }
        }

        return missing;
    }



    // ---------------- INSCRIPTIONS (ADMIN) ----------------

    public Inscription inscrire(Utilisateur acteurAdmin, UUID examenId, UUID etudiantId) {
        requireRole(acteurAdmin, Role.ADMIN);
        Examen ex = getExamenOrThrow(examenId);

        Utilisateur u = utilisateurs.get(etudiantId);
        if (!(u instanceof Etudiant)) throw new IllegalArgumentException("Référence étudiant invalide");

        Etudiant etu = (Etudiant) u;
        Inscription ins = ex.inscrire(etu);

        log.info(() -> String.format("[ADMIN] Inscription admin=%s -> examen=%s etu=%s",
                acteurAdmin.getCode(), ex.getCode(), etu.getCode()));

        return ins;
    }

    // ---------------- TENTATIVES (ETUDIANT) ----------------

    public Tentative demarrerTentative(Utilisateur acteurEtudiant, UUID examenId, LocalDateTime now) {
        requireRole(acteurEtudiant, Role.ETUDIANT);
        Examen ex = getExamenOrThrow(examenId);

        Inscription ins = ex.trouverInscription(acteurEtudiant.getId());
        if (ins == null || !ins.estActive()) throw new IllegalStateException("Non inscrit ou inscription inactive");
        if (!ex.estOuvert(now)) throw new IllegalStateException("Examen non ouvert / hors période");

        String cle = cleTentative(examenId, acteurEtudiant.getId());

        UUID deja = tentativeActiveParCle.get(cle);
        if (deja != null) {
            Tentative tExist = tentatives.get(deja);
            if (tExist != null) return tExist;
            tentativeActiveParCle.remove(cle);
            cleParTentative.remove(deja);
        }

        Tentative t = new Tentative(ex, (Etudiant) acteurEtudiant);
        t.demarrer(now);
        tentatives.put(t.getId(), t);

        tentativeActiveParCle.put(cle, t.getId());
        cleParTentative.put(t.getId(), cle);

        log.info(() -> String.format("[DEMARRER_EXAMEN] Début tentative id=%s examen=%s etu=%s",
                t.getId(), ex.getCode(), acteurEtudiant.getCode()));

        return t;
    }

    public void sauvegarderReponse(Utilisateur acteurEtudiant, UUID tentativeId, UUID questionId, String contenu, LocalDateTime now) {
        requireRole(acteurEtudiant, Role.ETUDIANT);
        Tentative t = getTentativeOrThrow(tentativeId);

        if (!t.getEtudiant().getId().equals(acteurEtudiant.getId()))
            throw new SecurityException("Tentative n'appartient pas à cet étudiant");

        t.sauvegarderReponse(questionId, contenu, now);
    }

    public void soumettreTentative(Utilisateur acteurEtudiant, UUID tentativeId, LocalDateTime now) {
        requireRole(acteurEtudiant, Role.ETUDIANT);
        Tentative t = getTentativeOrThrow(tentativeId);

        if (!t.getEtudiant().getId().equals(acteurEtudiant.getId()))
            throw new SecurityException("Tentative n'appartient pas à cet étudiant");

        t.soumettre(now);

        // ✅ après soumission: la tentative n'est plus "active" pour (examen+etu)
        String cle = cleParTentative.remove(tentativeId);
        if (cle != null && tentativeId.equals(tentativeActiveParCle.get(cle))) {
            tentativeActiveParCle.remove(cle);
        }
    }

    // ---------------- HELPERS ----------------

    private void requireRole(Utilisateur acteur, Role role) {
        if (acteur == null) throw new SecurityException("Utilisateur non authentifié");
        if (acteur.getRole() != role) throw new SecurityException("Accès refusé: rôle requis=" + role);
    }

    private Examen getExamenOrThrow(UUID examenId) {
        Examen ex = examens.get(examenId);
        if (ex == null) throw new IllegalArgumentException("Examen introuvable: " + examenId);
        return ex;
    }

    private Tentative getTentativeOrThrow(UUID tentativeId) {
        Tentative t = tentatives.get(tentativeId);
        if (t == null) throw new IllegalArgumentException("Tentative introuvable: " + tentativeId);
        return t;
    }

    // ---------- Code permanent ----------
    private static String normalizeLetters(String s) {
        if (s == null) return "";
        String up = s.trim().toUpperCase(Locale.ROOT);
        up = java.text.Normalizer.normalize(up, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        up = up.replaceAll("[^A-Z]", "");
        return up;
    }

    private static String codePermanentBase(String prenom, String nom, LocalDate dateNaissance) {
        String n = normalizeLetters(nom);
        String p = normalizeLetters(prenom);

        String n3 = (n.length() >= 3) ? n.substring(0, 3) : String.format("%-3s", n).replace(' ', 'X');
        String p1 = (p.length() >= 1) ? p.substring(0, 1) : "X";

        String dd = String.format("%02d", dateNaissance.getDayOfMonth());
        String mm = String.format("%02d", dateNaissance.getMonthValue());
        String yy2 = String.format("%02d", dateNaissance.getYear() % 100);

        return n3 + p1 + dd + mm + yy2; // sans suffixe
    }

    private String codePermanentUnique(String prenom, String nom, LocalDate dn) {
        String base = codePermanentBase(prenom, nom, dn);

        // on garde ton modèle "...03" en premier
        for (int suffix = 3; suffix <= 99; suffix++) {
            String cp = base + String.format("%02d", suffix);
            if (trouverUtilisateurParCodePermanent(cp) == null) return cp;
        }

        // fallback ultra rare
        return base + UUID.randomUUID().toString().substring(0, 2).toUpperCase(Locale.ROOT);
    }
}
