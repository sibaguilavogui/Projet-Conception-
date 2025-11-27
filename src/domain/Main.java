package domain;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {
    private static final Scanner sc = new Scanner(System.in);

    // ===== Tentatives: UUID -> ID simple (#1, #2, #3) =====
    private static int nextTentativeNo = 1;
    private static final Map<UUID, Integer> tentativeNoParId = new HashMap<>();
    private static int noTentative(UUID id) {
        return tentativeNoParId.computeIfAbsent(id, k -> nextTentativeNo++);
    }

    public static void main(String[] args) {
        ExamGuSystem sys = new ExamGuSystem();

        // ADMIN unique pré-défini (existe déjà)
        final String ADMIN_EMAIL = "admin@uqac.ca";
        final String ADMIN_MDP   = "admin";

        sys.creerUtilisateurBootstrapSilencieux(new Admin(ADMIN_EMAIL, ADMIN_MDP));

        System.out.println("=== EXAM-GU (Console) ===\n");

        // ✅ Login 3 rôles (ADMIN/ENSEIGNANT/ETUDIANT)
        while (true) {
            System.out.println("=== CONNEXION ===");
            System.out.println("1) ADMIN");
            System.out.println("2) ENSEIGNANT");
            System.out.println("3) ETUDIANT");
            System.out.println("0) Quitter");
            String choice = readLine("> ").trim();

            if ("0".equals(choice)) break;

            // ✅ Message si aucun prof/etu n'existe encore
            if (("2".equals(choice) || "3".equals(choice)) && !existeAuMoinsUnCompteNonAdmin(sys)) {
                System.out.println("❌ Aucun enseignant/étudiant n’existe encore.");
                System.out.println("➡️ Connecte-toi en ADMIN pour créer des comptes.\n");
                continue;
            }

            Utilisateur u = switch (choice) {
                case "1" -> loginRoleFlow(sys, Role.ADMIN);
                case "2" -> loginRoleFlow(sys, Role.ENSEIGNANT);
                case "3" -> loginRoleFlow(sys, Role.ETUDIANT);
                default -> null;
            };

            if (u == null) {
                System.out.println();
                continue;
            }

            switch (u.getRole()) {
                case ADMIN -> menuAdmin(sys, (Admin) u);
                case ENSEIGNANT -> menuEnseignant(sys, (Enseignant) u);
                case ETUDIANT -> menuEtudiant(sys, (Etudiant) u);
            }
        }

        System.out.println("Bye 👋");
    }

    // ================= LOGIN (nouveau: par rôle) =================
    private static boolean existeAuMoinsUnCompteNonAdmin(ExamGuSystem sys) {
        for (Utilisateur u : sys.listerUtilisateurs()) {
            if (u != null && u.getRole() != Role.ADMIN) return true;
        }
        return false;
    }

    private static Utilisateur loginRoleFlow(ExamGuSystem sys, Role role) {
        while (true) {
            System.out.println("=== LOGIN " + role + " ===");
            String email = readLine("Email (vide=retour): ").trim();
            if (email.isBlank()) return null;

            String mdp = readLine("Mot de passe: ").trim();
            Utilisateur u = sys.authentifier(email, mdp);

            if (u != null && u.getRole() == role) {
                System.out.println("✅ Connecté: " + u.getCode() + " (" + u.getRole() + ")\n");
                return u;
            }
            System.out.println("❌ Identifiants invalides ou rôle incorrect.\n");
        }
    }

    // ================= LOGIN (anciens) =================
    // (je les garde pour que tu ne perdes rien, même si on ne les utilise plus)
    private static Utilisateur loginFlow(ExamGuSystem sys) {
        String email = readLine("Email (vide=quit): ").trim();
        if (email.isBlank()) return null;
        String mdp = readLine("Mot de passe: ").trim();

        Utilisateur u = sys.authentifier(email, mdp);
        if (u == null) {
            System.out.println("Login invalide.\n");
            return loginFlow(sys);
        }
        System.out.println("✅ Connecté: " + u.getCode() + " (" + u.getRole() + ")\n");
        return u;
    }

    private static Admin loginAdminFlow(ExamGuSystem sys) {
        while (true) {
            System.out.println("=== LOGIN ADMIN ===");
            String email = readLine("Email (vide=quit): ");
            if (email.isBlank()) return null;
            String mdp = readLine("Mot de passe: ");

            Utilisateur u = sys.authentifier(email, mdp);
            if (u instanceof Admin a) {
                System.out.println("✅ Connecté ADMIN\n");
                return a;
            }

            System.out.println("❌ Accès refusé (admin requis).\n");
        }
    }

    // ================= ADMIN =================
    private static void menuAdmin(ExamGuSystem sys, Admin admin) {
        while (true) {
            System.out.println("=== MENU ADMIN ===");
            System.out.println("1) Créer utilisateur (ENSEIGNANT / ETUDIANT)");
            System.out.println("2) Lister examens");
            System.out.println("3) Inscrire un étudiant à un examen");
            System.out.println("4) Supprimer un utilisateur (ENSEIGNANT / ETUDIANT)");
            System.out.println("0) Déconnexion");
            String c = readLine("> ").trim();

            switch (c) {
                case "1" -> creerUtilisateurFlow(sys, admin);
                case "2" -> listerExamens(sys);
                case "3" -> inscrireFlow(sys, admin);
                case "4" -> supprimerUtilisateurFlow(sys, admin);
                case "0" -> { System.out.println(); return; }
                default -> System.out.println("Choix invalide.\n");
            }
        }
    }

    private static void creerUtilisateurFlow(ExamGuSystem sys, Admin adminActeur) {
        String r = readNonEmpty("Rôle (1=ENSEIGNANT, 2=ETUDIANT): ");
        String email = readNonEmpty("Email: ");
        String mdp = readNonEmpty("Mot de passe: ");

        // ✅ Infos nécessaires (codePermanent)
        String prenom = readNonEmpty("Prénom: ");
        String nom = readNonEmpty("Nom: ");
        String departement = readNonEmpty("Département: ");
        LocalDate dn = readDate("Date de naissance (YYYY-MM-DD): ");

        Utilisateur u;
        if ("1".equals(r)) u = new Enseignant(email, mdp, prenom, nom, departement, dn);
        else if ("2".equals(r)) u = new Etudiant(email, mdp, prenom, nom, departement, dn);
        else { System.out.println("Rôle invalide.\n"); return; }

        try {
            sys.creerUtilisateur(adminActeur, u);

            String cp = (u.getCodePermanent() == null) ? "" : (" | CP=" + u.getCodePermanent());
            System.out.println("✅ Utilisateur créé: " + u.getCode() + " (" + u.getRole() + ") " + u.getEmail() + cp + "\n");
        } catch (RuntimeException e) {
            System.out.println("Erreur: " + e.getMessage() + "\n");
        }
    }

    private static LocalDate readDate(String prompt) {
        while (true) {
            String s = readNonEmpty(prompt);
            try {
                return LocalDate.parse(s.trim());
            } catch (DateTimeParseException e) {
                System.out.println("Format invalide. Exemple: 2002-10-01");
            }
        }
    }

    private static void inscrireFlow(ExamGuSystem sys, Admin admin) {
        Examen ex = readExamen(sys);
        if (ex == null) return;

        String etuRef = readNonEmpty("Etudiant (ETU-xxxx ou UUID ou email): ");
        Utilisateur u = sys.trouverUtilisateurParRef(etuRef);
        if (u == null) u = sys.trouverUtilisateurParEmail(etuRef);

        if (!(u instanceof Etudiant)) {
            System.out.println("❌ Étudiant introuvable.\n");
            return;
        }

        try {
            sys.inscrire(admin, ex.getId(), u.getId());
            System.out.println("✅ Inscription OK: " + u.getCode() + " -> " + ex.getCode() + "\n");
        } catch (RuntimeException e) {
            System.out.println("Erreur: " + e.getMessage() + "\n");
        }
    }

    private static void supprimerUtilisateurFlow(ExamGuSystem sys, Admin admin) {
        System.out.println("=== SUPPRIMER UTILISATEUR ===");

        int nb = 0;
        for (Utilisateur u : sys.listerUtilisateurs()) {
            if (u.getRole() == Role.ADMIN) continue;
            nb++;
            String cp = (u.getCodePermanent() == null) ? "" : (" | CP=" + u.getCodePermanent());
            System.out.println("- " + u.getCode() + " (" + u.getRole() + ") " + u.getEmail() + cp);
        }

        if (nb == 0) {
            System.out.println("(Aucun enseignant/étudiant à supprimer)\n");
            return;
        }

        String ref = readNonEmpty("Utilisateur à supprimer (code / codePermanent / uuid / email): ");

        try {
            Utilisateur suppr = sys.supprimerUtilisateur(admin, ref);
            System.out.println("✅ Supprimé: " + suppr.getCode() + " (" + suppr.getRole() + ")\n");
        } catch (RuntimeException e) {
            System.out.println("Erreur: " + e.getMessage() + "\n");
        }
    }

    // ================= ENSEIGNANT =================
    private static void menuEnseignant(ExamGuSystem sys, Enseignant prof) {
        while (true) {
            System.out.println("=== MENU ENSEIGNANT ===");
            System.out.println("1) Créer examen + ajouter questions (wizard)");
            System.out.println("2) Planifier + ouvrir un examen");
            System.out.println("3) Corriger COURTE (note + commentaire)");
            System.out.println("4) Publier notes d’un examen");
            System.out.println("0) Déconnexion");
            String c = readLine("> ").trim();

            switch (c) {
                case "1" -> creerExamenWizard(sys, prof);
                case "2" -> ouvrirExamenFlow(sys, prof);
                case "3" -> corrigerCourtesAvecCommentaire(sys, prof);
                case "4" -> publierNotesFlow(sys, prof);
                case "0" -> { System.out.println(); return; }
                default -> System.out.println("Choix invalide.\n");
            }
        }
    }

    private static void creerExamenWizard(ExamGuSystem sys, Enseignant prof) {
        String titre = readNonEmpty("Titre: ");
        Examen ex;
        try {
            ex = sys.creerExamen(prof, titre);
            System.out.println("✅ Examen créé: " + ex.getCode() + "\n");
        } catch (RuntimeException e) {
            System.out.println("Erreur: " + e.getMessage() + "\n");
            return;
        }

        // Ajout questions en boucle (sans sortir)
        ajouterQuestionsWizard(sys, prof, ex);

        // Option d’ouverture
        String o = readLine("Ouvrir maintenant ? (o/n): ").trim().toLowerCase();
        if (o.startsWith("o")) {
            int duree = readInt("Durée (minutes) ex: 30 : ", 1, 100000);
            int fenetre = readInt("Fenêtre ouverte (minutes) ex: 60 : ", 1, 100000);
            LocalDateTime debut = LocalDateTime.now();
            LocalDateTime fin = debut.plusMinutes(fenetre);
            try {
                sys.planifierEtOuvrirExamen(prof, ex.getId(), debut, fin, duree);
                System.out.println("✅ Examen OUVERT: " + ex.getCode() + "\n");
            } catch (RuntimeException e) {
                System.out.println("Erreur: " + e.getMessage() + "\n");
            }
        } else {
            System.out.println("ℹ️ Examen reste en BROUILLON.\n");
        }
    }

    private static void ajouterQuestionsWizard(ExamGuSystem sys, Enseignant prof, Examen ex) {
        System.out.println("=== AJOUT QUESTIONS (" + ex.getCode() + ") ===");
        while (true) {
            System.out.println("Type: 1) QCM  2) COURTE  3) VRAI/FAUX  0) Terminer");
            String type = readLine("> ").trim();
            if ("0".equals(type)) { System.out.println("✅ Fin ajout questions.\n"); return; }

            String enonce = readNonEmpty("Énoncé: ");
            double bareme = readDouble("Barème (points): ", 0.000001, 1_000_000);

            Question q;

            if ("1".equals(type)) {
                q = new Question(enonce, TypeQuestion.QCM, bareme);

                int n = readInt("Nombre de choix (>=2): ", 2, 20);
                List<String> labels = new ArrayList<>();
                for (int i = 1; i <= n; i++) labels.add(readNonEmpty("Choix " + i + ": "));

                String corr = readNonEmpty("Indice(s) correct(s) ex: 2 ou 1,3 : ");
                Set<Integer> correctIdx = parseIndexSet(corr);

                for (int i = 1; i <= n; i++) {
                    boolean ok = correctIdx.contains(i);
                    q.ajouterReponsePossible(new ReponsePossible(labels.get(i - 1), ok));
                }

            } else if ("2".equals(type)) {
                q = new Question(enonce, TypeQuestion.COURTE, bareme);
                System.out.println("✅ COURTE: correction manuelle + commentaire.");

            } else if ("3".equals(type)) {
                q = new Question(enonce, TypeQuestion.VRAI_FAUX, bareme);
                String bonne = readNonEmpty("Bonne réponse (vrai/faux): ").trim().toLowerCase();
                boolean vraiEstCorrect = bonne.startsWith("v");
                q.ajouterReponsePossible(new ReponsePossible("vrai", vraiEstCorrect));
                q.ajouterReponsePossible(new ReponsePossible("faux", !vraiEstCorrect));

            } else {
                System.out.println("Type invalide.\n");
                continue;
            }

            try {
                sys.ajouterQuestion(prof, ex.getId(), q);
                System.out.println("✅ Question ajoutée.\n");
            } catch (RuntimeException e) {
                System.out.println("Erreur: " + e.getMessage() + "\n");
            }
        }
    }

    private static void ouvrirExamenFlow(ExamGuSystem sys, Enseignant prof) {
        Examen ex = readExamen(sys);
        if (ex == null) return;

        int duree = readInt("Durée (minutes) ex: 30 : ", 1, 100000);
        int fenetre = readInt("Fenêtre ouverte (minutes) ex: 60 : ", 1, 100000);

        LocalDateTime debut = LocalDateTime.now();
        LocalDateTime fin = debut.plusMinutes(fenetre);

        try {
            sys.planifierEtOuvrirExamen(prof, ex.getId(), debut, fin, duree);
            System.out.println("✅ Examen planifié + ouvert: " + ex.getCode() + "\n");
        } catch (RuntimeException e) {
            System.out.println("Erreur: " + e.getMessage() + "\n");
        }
    }

    private static void corrigerCourtesAvecCommentaire(ExamGuSystem sys, Enseignant prof) {
        // Liste "à corriger" : tous COURTE des tentatives
        List<ItemCourte> items = new ArrayList<>();

        for (Tentative t : sys.listerTentatives()) {
            if (t == null || t.getExamen() == null || t.getEtudiant() == null) continue;

            Examen ex = t.getExamen();

            // (optionnel) limiter au prof créateur
            if (ex.getCreateurId() != null && !ex.getCreateurId().equals(prof.getId())) continue;

            for (Question q : ex.getQuestions()) {
                if (q == null || q.getType() != TypeQuestion.COURTE) continue;

                String rep = reponseEtudiantTexte(t, q.getId());
                if (rep == null) continue; // pas répondu

                items.add(new ItemCourte(t, ex, q, rep));
            }
        }

        if (items.isEmpty()) {
            System.out.println("Aucune question COURTE à corriger.\n");
            return;
        }

        System.out.println("=== COURTES À CORRIGER ===");
        for (int i = 0; i < items.size(); i++) {
            ItemCourte it = items.get(i);
            Double note = it.tentative.getNoteManuelle(it.question.getId());
            String comm = it.tentative.getCommentaireManuel(it.question.getId());
            System.out.println((i + 1) + ") " +
                    it.examen.getCode() + " | etu=" + it.tentative.getEtudiant().getCode() +
                    " | tentative #" + noTentative(it.tentative.getId()) +
                    " | note=" + (note == null ? "(non corrigé)" : note + "/" + it.question.getBareme()) +
                    (comm == null ? "" : " | comm=OK"));
        }

        int choix = readInt("Choix # : ", 1, items.size());
        ItemCourte it = items.get(choix - 1);

        System.out.println("\nExamen: " + it.examen.getCode() + " - " + it.examen.getTitre());
        System.out.println("Étudiant: " + it.tentative.getEtudiant().getCode() + " (" + it.tentative.getEtudiant().getEmail() + ")");
        System.out.println("Question: " + it.question.getEnonce());
        System.out.println("Réponse étudiant: " + it.reponseEtu);

        double note = readDouble("Note (0 .. " + it.question.getBareme() + "): ", 0.0, it.question.getBareme());
        String commentaire = readLine("Commentaire (optionnel): ");

        try {
            it.tentative.noterManuellement(it.question.getId(), note, commentaire);
            System.out.println("✅ Corrigé. Score total (incluant COURTE) = " +
                    String.format(Locale.US, "%.2f", it.tentative.getScore()) +
                    " / " + String.format(Locale.US, "%.2f", it.examen.totalPoints()) + "\n");
        } catch (RuntimeException e) {
            System.out.println("Erreur: " + e.getMessage() + "\n");
        }
    }

    private static void publierNotesFlow(ExamGuSystem sys, Enseignant prof) {
        Examen ex = readExamen(sys);
        if (ex == null) return;

        try {
            sys.publierNotes(prof, ex.getId(), LocalDateTime.now());
            System.out.println("✅ Notes publiées pour " + ex.getCode() + "\n");
        } catch (RuntimeException e) {
            System.out.println("Erreur: " + e.getMessage() + "\n");
        }
    }

    private static class ItemCourte {
        Tentative tentative;
        Examen examen;
        Question question;
        String reponseEtu;

        ItemCourte(Tentative t, Examen ex, Question q, String rep) {
            this.tentative = t;
            this.examen = ex;
            this.question = q;
            this.reponseEtu = rep;
        }
    }

    // ================= ÉTUDIANT =================
    private static void menuEtudiant(ExamGuSystem sys, Etudiant etu) {
        while (true) {
            System.out.println("=== MENU ETUDIANT ===");
            System.out.println("1) Mes examens (inscrits)");
            System.out.println("2) Démarrer/reprendre une tentative (inscrit + ouvert) et répondre");
            System.out.println("3) Voir ma note (par examen) + détail + commentaire");
            System.out.println("0) Déconnexion");
            String c = readLine("> ").trim();

            switch (c) {
                case "1" -> mesExamens(sys, etu);
                case "2" -> demarrerEtPasserTentative(sys, etu);
                case "3" -> voirNoteDetailFlow(sys, etu);
                case "0" -> { System.out.println(); return; }
                default -> System.out.println("Choix invalide.\n");
            }
        }
    }

    private static void mesExamens(ExamGuSystem sys, Etudiant etu) {
        System.out.println("=== EXAMENS INSCRITS ===");
        int i = 0;
        for (Examen ex : sys.listerExamens()) {
            Inscription ins = ex.trouverInscription(etu.getId());
            if (ins != null && ins.estActive()) {
                System.out.println(++i + ") " + ex.getCode() + " | " + ex.getEtat() + " | " + ex.getTitre());
            }
        }
        if (i == 0) System.out.println("(aucun)\n");
        else System.out.println();
    }

    // ✅ (1) Soumission confirmée par l’étudiant + reprise possible
    // ✅ (3) pas d’affichage de score côté étudiant après soumission
    private static void demarrerEtPasserTentative(ExamGuSystem sys, Etudiant etu) {
        LocalDateTime now = LocalDateTime.now();

        List<Examen> eligibles = new ArrayList<>();
        for (Examen ex : sys.listerExamens()) {
            Inscription ins = ex.trouverInscription(etu.getId());
            if (ins != null && ins.estActive() && ex.estOuvert(now)) eligibles.add(ex);
        }

        if (eligibles.isEmpty()) {
            System.out.println("❌ Aucun examen ouvert pour lequel tu es inscrit.\n");
            return;
        }

        System.out.println("Choisir un examen (inscrit + ouvert):");
        for (int i = 0; i < eligibles.size(); i++) {
            Examen ex = eligibles.get(i);
            System.out.println((i + 1) + ") " + ex.getCode() + " | " + ex.getTitre());
        }
        System.out.println("Tu peux aussi taper directement EX-0001.");

        String s = readNonEmpty("Choix (# ou EX-xxxx): ");
        Examen choisi = null;

        if (s.matches("\\d+")) {
            int idx = Integer.parseInt(s);
            if (idx >= 1 && idx <= eligibles.size()) choisi = eligibles.get(idx - 1);
        } else {
            String ref = s.trim().toUpperCase();
            for (Examen ex : eligibles) {
                if (ref.equals(ex.getCode())) { choisi = ex; break; }
            }
        }

        if (choisi == null) {
            System.out.println("Choix invalide.\n");
            return;
        }

        Tentative t;
        boolean reprise = false;

        try {
            t = sys.demarrerTentative(etu, choisi.getId(), now);
            reprise = (t.getReponses() != null && !t.getReponses().isEmpty());
        } catch (RuntimeException e) {
            System.out.println("Erreur: " + e.getMessage() + "\n");
            return;
        }

        int no = noTentative(t.getId());
        System.out.println((reprise ? "🔁 Tentative reprise: #" : "✅ Tentative démarrée: #") + no +
                " (" + choisi.getCode() + ")\n");

        passerQuestions(sys, etu, t);

        String ans = readLine("Soumettre maintenant ? (o/n): ").trim().toLowerCase();
        if (ans.startsWith("o")) {
            try {
                sys.soumettreTentative(etu, t.getId(), LocalDateTime.now());
                System.out.println("✅ Tentative soumise.");
                System.out.println("ℹ️ La note sera visible seulement quand l’enseignant publie les notes.\n");
            } catch (RuntimeException e) {
                System.out.println("Erreur soumission: " + e.getMessage() + "\n");
            }
        } else {
            System.out.println("💾 Réponses sauvegardées. Tu peux revenir plus tard pour soumettre.\n");
        }
    }

    // ✅ Reprise: affiche réponse actuelle et propose de modifier
    private static void passerQuestions(ExamGuSystem sys, Etudiant etu, Tentative t) {
        Examen ex = t.getExamen();
        List<Question> qs = ex.getQuestions();
        if (qs.isEmpty()) {
            System.out.println("⚠️ Examen sans questions.\n");
            return;
        }

        for (int i = 0; i < qs.size(); i++) {
            Question q = qs.get(i);

            System.out.println("----------------------------------------");
            System.out.println("Q" + (i + 1) + "/" + qs.size() + " (" + q.getType() + ", " + q.getBareme() + " pts)");
            System.out.println(q.getEnonce());

            String deja = reponseEtudiantTexte(t, q.getId());
            if (deja != null && !deja.isBlank()) {
                System.out.println("Réponse actuelle: " + deja);
                String mod = readLine("Modifier ? (o/n): ").trim().toLowerCase();
                if (!mod.startsWith("o")) {
                    continue;
                }
            }

            String contenu = "";

            if (q.getType() == TypeQuestion.QCM) {
                List<ReponsePossible> reps = q.getReponsesPossibles();
                for (int k = 0; k < reps.size(); k++) {
                    System.out.println((k + 1) + ") " + reps.get(k).getLibelle());
                }
                int ch = readInt("Choix #: ", 1, reps.size());
                contenu = reps.get(ch - 1).getLibelle();

            } else if (q.getType() == TypeQuestion.VRAI_FAUX) {
                System.out.println("1) vrai");
                System.out.println("2) faux");
                int ch = readInt("Choix #: ", 1, 2);
                contenu = (ch == 1 ? "vrai" : "faux");

            } else if (q.getType() == TypeQuestion.COURTE) {
                contenu = readNonEmpty("Réponse (texte): ");
            }

            sys.sauvegarderReponse(etu, t.getId(), q.getId(), contenu, LocalDateTime.now());
        }

        System.out.println("----------------------------------------\n");
    }

    private static void voirNoteDetailFlow(ExamGuSystem sys, Etudiant etu) {
        Examen ex = readExamen(sys);
        if (ex == null) return;

        Inscription ins = ex.trouverInscription(etu.getId());
        if (ins == null || !ins.estActive()) {
            System.out.println("❌ Tu n’es pas inscrit à cet examen.\n");
            return;
        }

        // ✅ (3) Notes visibles seulement si publiées par l’enseignant
        NotePublication pub = ex.getPublication();
        if (pub == null || !pub.estPubliee()) {
            System.out.println("❌ Notes non publiées pour " + ex.getCode() + ".\n");
            return;
        }

        Tentative last = null;
        for (Tentative t : sys.listerTentatives()) {
            if (t == null || t.getExamen() == null || t.getEtudiant() == null) continue;
            if (!t.getExamen().getId().equals(ex.getId())) continue;
            if (!t.getEtudiant().getId().equals(etu.getId())) continue;

            if (last == null) last = t;
            else {
                LocalDateTime a = (t.getFin() != null) ? t.getFin() : t.getDebut();
                LocalDateTime b = (last.getFin() != null) ? last.getFin() : last.getDebut();
                if (a != null && b != null && a.isAfter(b)) last = t;
            }
        }

        if (last == null) {
            System.out.println("❌ Aucune tentative trouvée.\n");
            return;
        }

        System.out.println("✅ NOTE FINALE (" + ex.getCode() + " - " + ex.getTitre() + ")");
        System.out.println("Tentative #" + noTentative(last.getId()));
        System.out.println("Total = " + String.format(Locale.US, "%.2f", last.getScore()) +
                " / " + String.format(Locale.US, "%.2f", ex.totalPoints()));
        System.out.println("\n=== DÉTAIL ===");

        for (Question q : ex.getQuestions()) {
            String rep = reponseEtudiantTexte(last, q.getId());
            if (rep == null) rep = "(aucune réponse)";

            double note;
            String commentaire = null;

            if (q.getType() == TypeQuestion.COURTE) {
                Double m = last.getNoteManuelle(q.getId());
                note = (m == null ? 0.0 : m);
                commentaire = last.getCommentaireManuel(q.getId());
            } else {
                note = q.corriger(rep.equals("(aucune réponse)") ? null : rep);
            }

            System.out.println("----------------------------------------");
            System.out.println("Question : " + q.getEnonce());
            System.out.println("Type     : " + q.getType());
            System.out.println("Réponse  : " + rep);
            System.out.println("Note     : " + String.format(Locale.US, "%.2f", note) + " / " + q.getBareme());

            if (q.getType() == TypeQuestion.COURTE) {
                System.out.println("Correction: MANUELLE");
                if (commentaire != null && !commentaire.isBlank()) {
                    System.out.println("Commentaire prof: " + commentaire);
                }
            }
        }

        System.out.println("----------------------------------------\n");
    }

    // ================= HELPERS =================
    private static String reponseEtudiantTexte(Tentative t, UUID questionId) {
        for (ReponseDonnee rd : t.getReponses()) {
            if (rd != null && rd.getQuestion() != null && questionId.equals(rd.getQuestion().getId())) {
                return rd.getContenu();
            }
        }
        return null;
    }

    private static void listerExamens(ExamGuSystem sys) {
        Collection<Examen> exams = sys.listerExamens();
        if (exams.isEmpty()) { System.out.println("Aucun examen.\n"); return; }
        System.out.println("=== EXAMENS ===");
        for (Examen ex : exams) {
            System.out.println("- " + ex.getCode() + " | " + ex.getEtat() + " | " + ex.getTitre());
        }
        System.out.println();
    }

    private static Examen readExamen(ExamGuSystem sys) {
        String ref = readNonEmpty("Examen (EX-0001 ou UUID): ");
        Examen ex = sys.trouverExamenParRef(ref);
        if (ex == null) System.out.println("Examen introuvable.\n");
        return ex;
    }

    private static Set<Integer> parseIndexSet(String s) {
        Set<Integer> out = new HashSet<>();
        for (String part : s.split(",")) {
            String t = part.trim();
            if (t.matches("\\d+")) out.add(Integer.parseInt(t));
        }
        return out;
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine();
    }

    private static String readNonEmpty(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (s != null && !s.trim().isEmpty()) return s.trim();
            System.out.println("Valeur obligatoire.");
        }
    }

    private static int readInt(String prompt, int min, int max) {
        while (true) {
            String s = readNonEmpty(prompt);
            try {
                int v = Integer.parseInt(s);
                if (v < min || v > max) { System.out.println("Entre " + min + " et " + max + "."); continue; }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Nombre invalide.");
            }
        }
    }

    private static double readDouble(String prompt, double min, double max) {
        while (true) {
            String s = readNonEmpty(prompt);
            try {
                double v = Double.parseDouble(s.replace(',', '.'));
                if (v < min || v > max) { System.out.println("Entre " + min + " et " + max + "."); continue; }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Nombre invalide.");
            }
        }
    }
}
