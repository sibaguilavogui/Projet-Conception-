package ca.uqac.examgu.api;

import ca.uqac.examgu.application.ExamGuSystem;
import ca.uqac.examgu.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/etudiants")
public class EtudiantController {

    private final ExamGuSystem examGuSystem;

    public EtudiantController(ExamGuSystem examGuSystem) {
        this.examGuSystem = examGuSystem;
    }

    // =========================================================
    // 1. Liste des examens où l’étudiant est INSCRIT
    // =========================================================
    @GetMapping("/examens")
    public ResponseEntity<?> mesExamens(@RequestHeader("X-User-Id") String etuRef) {
        Utilisateur u = examGuSystem.trouverUtilisateurParRef(etuRef);
        if (!(u instanceof Etudiant etu)) {
            return ResponseEntity.status(403).body("Accès refusé : ETUDIANT requis.");
        }

        List<ExamenInscritDto> res = new ArrayList<>();
        for (Examen ex : examGuSystem.listerExamens()) {
            Inscription ins = ex.trouverInscription(etu.getId());
            if (ins != null && ins.estActive()) {
                res.add(new ExamenInscritDto(
                        ex.getId(),
                        ex.getCode(),
                        ex.getEtat(),
                        ex.getTitre()
                ));
            }
        }
        return ResponseEntity.ok(res);
    }

    // =========================================================
    // 2. Démarrer / reprendre une tentative
    //    (le front appelle ensuite /tentatives/{id}/questions)
    // =========================================================
    @PostMapping("/examens/{examenRef}/tentatives")
    public ResponseEntity<?> demarrerTentative(
            @RequestHeader("X-User-Id") String etuRef,
            @PathVariable String examenRef
    ) {
        Utilisateur u = examGuSystem.trouverUtilisateurParRef(etuRef);
        if (!(u instanceof Etudiant etu)) {
            return ResponseEntity.status(403).body("Accès refusé : ETUDIANT requis.");
        }

        Examen ex = examGuSystem.trouverExamenParRef(examenRef);
        if (ex == null) {
            return ResponseEntity.badRequest().body("Examen introuvable : " + examenRef);
        }

        try {
            Tentative t = examGuSystem.demarrerTentative(etu, ex.getId(), LocalDateTime.now());

            // ======= Calcul du temps restant =======
            LocalDateTime now = LocalDateTime.now();
            int dureeMinutes = ex.getDureeMinutes();
            LocalDateTime limite = t.getDebut().plusMinutes(dureeMinutes);

            long tempsRestantSecondes = Duration.between(now, limite).getSeconds();
            if (tempsRestantSecondes < 0) tempsRestantSecondes = 0;

            TentativeDto dto = new TentativeDto(
                    t.getId(),
                    ex.getId(),
                    ex.getCode(),
                    ex.getTitre(),
                    t.getDebut(),
                    t.getFin(),
                    dureeMinutes,
                    tempsRestantSecondes,
                    null   // les questions seront chargées via /tentatives/{id}/questions
            );

            return ResponseEntity.ok(dto);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================
    // 3. Lister les questions d’une tentative (pour l’étudiant)
    //    -> c’est l’endpoint que ton index.html appelle
    // =========================================================
    @GetMapping("/tentatives/{tentativeId}/questions")
    public ResponseEntity<?> questionsPourTentative(
            @RequestHeader("X-User-Id") String etuRef,
            @PathVariable UUID tentativeId
    ) {
        Utilisateur u = examGuSystem.trouverUtilisateurParRef(etuRef);
        if (!(u instanceof Etudiant etu)) {
            return ResponseEntity.status(403).body("Accès refusé : ETUDIANT requis.");
        }

        Tentative t = examGuSystem.trouverTentative(tentativeId);
        if (t == null) {
            return ResponseEntity.badRequest().body("Tentative introuvable : " + tentativeId);
        }
        if (t.getEtudiant() == null || !t.getEtudiant().getId().equals(etu.getId())) {
            return ResponseEntity.status(403).body("Cette tentative appartient à un autre étudiant.");
        }

        Examen ex = t.getExamen();
        if (ex == null) {
            return ResponseEntity.badRequest().body("Tentative sans examen associé.");
        }

        List<QuestionTentativeDto> resultat = new ArrayList<>();
        int numero = 1;

        for (Question q : ex.getQuestions()) {
            if (q == null) continue;

            List<String> choix = null;
            if (q.getType() == TypeQuestion.QCM || q.getType() == TypeQuestion.VRAI_FAUX) {
                choix = q.getReponsesPossibles()
                        .stream()
                        .map(ReponsePossible::getLibelle)
                        .toList();
            }

            resultat.add(new QuestionTentativeDto(
                    q.getId(),
                    numero++,
                    q.getType(),
                    q.getEnonce(),
                    q.getBareme(),
                    choix
            ));
        }

        return ResponseEntity.ok(resultat);
    }

    // =========================================================
    // 4. Sauvegarder toutes les réponses d’un coup
    //    (format envoyé par ton index.html : { reponses:[...] })
    // =========================================================
    @PostMapping("/tentatives/{tentativeId}/reponses")
    public ResponseEntity<?> sauvegarderReponses(
            @RequestHeader("X-User-Id") String etuRef,
            @PathVariable UUID tentativeId,
            @RequestBody SoumettreReponsesDto dto
    ) {
        Utilisateur u = examGuSystem.trouverUtilisateurParRef(etuRef);
        if (!(u instanceof Etudiant etu)) {
            return ResponseEntity.status(403).body("Accès refusé : ETUDIANT requis.");
        }

        if (dto == null || dto.reponses() == null) {
            return ResponseEntity.badRequest().body("Aucune réponse à sauvegarder.");
        }

        Tentative t = examGuSystem.trouverTentative(tentativeId);
        if (t == null) {
            return ResponseEntity.badRequest().body("Tentative introuvable : " + tentativeId);
        }
        if (t.getEtudiant() == null || !t.getEtudiant().getId().equals(etu.getId())) {
            return ResponseEntity.status(403).body("Cette tentative appartient à un autre étudiant.");
        }

        Examen ex = t.getExamen();
        if (ex == null) {
            return ResponseEntity.badRequest().body("Tentative sans examen associé.");
        }

        try {
            LocalDateTime now = LocalDateTime.now();

            for (ReponseQuestionDto r : dto.reponses()) {
                if (r == null || r.questionId() == null) continue;

                Question q = ex.trouverQuestion(r.questionId());
                if (q == null) continue;

                String contenu = null;

                // QCM / VRAI_FAUX : on convertit l’index du choix en texte (libellé)
                if ("QCM".equalsIgnoreCase(r.type()) || "VRAI_FAUX".equalsIgnoreCase(r.type())) {
                    Integer idx = r.reponseChoixIndex();
                    if (idx != null && idx >= 0) {
                        List<ReponsePossible> possibles = q.getReponsesPossibles();
                        if (idx < possibles.size()) {
                            contenu = possibles.get(idx).getLibelle();
                        }
                    }
                } else {
                    // COURTE / LONGUE : texte libre
                    if (r.reponseTexte() != null && !r.reponseTexte().isBlank()) {
                        contenu = r.reponseTexte().trim();
                    } else {
                        contenu = null;
                    }
                }

                examGuSystem.sauvegarderReponse(etu, tentativeId, q.getId(), contenu, now);
            }

            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================
    // 5. Soumettre la tentative (fin de l’examen)
    // =========================================================
    @PostMapping("/tentatives/{tentativeId}/soumettre")
    public ResponseEntity<?> soumettreTentative(
            @RequestHeader("X-User-Id") String etuRef,
            @PathVariable UUID tentativeId
    ) {
        Utilisateur u = examGuSystem.trouverUtilisateurParRef(etuRef);
        if (!(u instanceof Etudiant etu)) {
            return ResponseEntity.status(403).body("Accès refusé : ETUDIANT requis.");
        }

        try {
            examGuSystem.soumettreTentative(etu, tentativeId, LocalDateTime.now());
            return ResponseEntity.ok("Tentative soumise.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================
    // 6. Voir la note détaillée (si publiée)
    // =========================================================
    @GetMapping("/examens/{examenRef}/note")
    public ResponseEntity<?> voirNoteDetail(
            @RequestHeader("X-User-Id") String etuRef,
            @PathVariable String examenRef
    ) {
        Utilisateur u = examGuSystem.trouverUtilisateurParRef(etuRef);
        if (!(u instanceof Etudiant etu)) {
            return ResponseEntity.status(403).body("Accès refusé : ETUDIANT requis.");
        }

        Examen ex = examGuSystem.trouverExamenParRef(examenRef);
        if (ex == null) {
            return ResponseEntity.badRequest().body("Examen introuvable : " + examenRef);
        }

        Inscription ins = ex.trouverInscription(etu.getId());
        if (ins == null || !ins.estActive()) {
            return ResponseEntity.badRequest().body("Tu n'es pas inscrit à cet examen.");
        }

        NotePublication pub = ex.getPublication();
        if (pub == null || !pub.estPubliee()) {
            return ResponseEntity.badRequest().body("Notes non publiées pour " + ex.getCode());
        }

        // dernière tentative de cet étudiant pour cet examen
        Tentative last = null;
        for (Tentative t : examGuSystem.listerTentatives()) {
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
            return ResponseEntity.badRequest().body("Aucune tentative trouvée.");
        }

        double total = last.getScore();
        double totalPoints = ex.totalPoints();

        List<QuestionNoteDto> details = new ArrayList<>();
        for (Question q : ex.getQuestions()) {
            String rep = reponseEtudiantTexte(last, q.getId());
            if (rep == null) rep = "(aucune réponse)";

            double note;
            String commentaire = null;

            if (q.getType() == TypeQuestion.COURTE || q.getType() == TypeQuestion.LONGUE) {
                Double m = last.getNoteManuelle(q.getId());
                note = (m == null ? 0.0 : m);
                commentaire = last.getCommentaireManuel(q.getId());
            } else {
                note = q.corriger(rep.equals("(aucune réponse)") ? null : rep);
            }

            details.add(new QuestionNoteDto(
                    q.getId(),
                    q.getEnonce(),
                    q.getType(),
                    q.getBareme(),
                    rep,
                    note,
                    commentaire
            ));
        }

        NoteDetailDto dto = new NoteDetailDto(
                ex.getId(),
                ex.getCode(),
                ex.getTitre(),
                last.getId(),
                total,
                totalPoints,
                details
        );

        return ResponseEntity.ok(dto);
    }

    // =========================================================
    // Helpers internes
    // =========================================================
    private static String reponseEtudiantTexte(Tentative t, UUID questionId) {
        for (ReponseDonnee rd : t.getReponses()) {
            if (rd != null && rd.getQuestion() != null
                    && questionId.equals(rd.getQuestion().getId())) {
                return rd.getContenu();
            }
        }
        return null;
    }

    // =========================================================
    // DTO
    // =========================================================

    public record ExamenInscritDto(
            UUID examenId,
            String code,
            EtatExamen etat,
            String titre
    ) {}

    public record TentativeDto(
            UUID tentativeId,
            UUID examenId,
            String examenCode,
            String examenTitre,
            LocalDateTime debut,
            LocalDateTime fin,
            int dureeMinutes,
            long tempsRestantSecondes,
            List<QuestionAvecReponseDto> questions
    ) {}

    public record QuestionAvecReponseDto(
            UUID questionId,
            String enonce,
            TypeQuestion type,
            double bareme,
            List<String> choix,
            String reponseActuelle
    ) {}

    // Pour l’appel GET /tentatives/{id}/questions
    public record QuestionTentativeDto(
            UUID id,
            int numero,
            TypeQuestion type,
            String enonce,
            double bareme,
            List<String> choix
    ) {}

    // Pour la soumission des réponses depuis le front
    public record ReponseQuestionDto(
            UUID questionId,
            String type,
            Integer reponseChoixIndex,
            String reponseTexte
    ) {}

    public record SoumettreReponsesDto(
            List<ReponseQuestionDto> reponses
    ) {}

    public record QuestionNoteDto(
            UUID questionId,
            String enonce,
            TypeQuestion type,
            double bareme,
            String reponse,
            double note,
            String commentaire
    ) {}

    public record NoteDetailDto(
            UUID examenId,
            String examenCode,
            String examenTitre,
            UUID tentativeId,
            double total,
            double totalPoints,
            List<QuestionNoteDto> questions
    ) {}
}
