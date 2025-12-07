

package ca.uqac.examgu.api;

import ca.uqac.examgu.application.ExamGuSystem;
import ca.uqac.examgu.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/enseignants")
public class EnseignantController {

    private final ExamGuSystem examGuSystem;

    // Petit stockage en mémoire pour les commentaires par tentative (côté enseignant)
    private final Map<UUID, List<CommentaireCopie>> commentairesParTentative = new ConcurrentHashMap<>();

    public EnseignantController(ExamGuSystem examGuSystem) {
        this.examGuSystem = examGuSystem;
    }

    // ---------- Utilitaire de sécurité ----------

    private Enseignant requireEnseignant(String ref) {
        Utilisateur u = examGuSystem.trouverUtilisateurParRef(ref);
        if (!(u instanceof Enseignant ens)) {
            throw new SecurityException("Accès refusé : ENSEIGNANT requis.");
        }
        return ens;
    }

    // =========================================================================
    //  EXAMENS : liste, création, questions, ouverture, publication, fermeture
    // =========================================================================

    @GetMapping("/examens")
    public ResponseEntity<?> listerExamens(@RequestHeader("X-User-Id") String ensRef) {
        try {
            Enseignant ens = requireEnseignant(ensRef);
            List<Examen> list = examGuSystem.listerExamensPourEnseignant(ens);

            List<ExamenDto> dtos = list.stream()
                    .map(ex -> new ExamenDto(
                            ex.getId(),
                            ex.getCode(),
                            ex.getTitre(),
                            ex.getEtat(),
                            ex.getDateDebut(),
                            ex.getDateFin(),
                            ex.getDureeMinutes()
                    ))
                    .toList();

            return ResponseEntity.ok(dtos);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping("/examens")
    public ResponseEntity<?> creerExamen(
            @RequestHeader("X-User-Id") String ensRef,
            @RequestBody CreerExamenDto dto
    ) {
        try {
            Enseignant ens = requireEnseignant(ensRef);
            String titre = Optional.ofNullable(dto.titre()).orElse("").trim();
            if (titre.isEmpty()) {
                return ResponseEntity.badRequest().body("Titre obligatoire.");
            }

            Examen ex = examGuSystem.creerExamen(ens, titre);
            return ResponseEntity.ok(new ExamenDto(
                    ex.getId(),
                    ex.getCode(),
                    ex.getTitre(),
                    ex.getEtat(),
                    ex.getDateDebut(),
                    ex.getDateFin(),
                    ex.getDureeMinutes()
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping("/examens/{code}/questions")
    public ResponseEntity<?> listerQuestionsExamen(
            @RequestHeader("X-User-Id") String ensRef,
            @PathVariable String code
    ) {
        try {
            Enseignant ens = requireEnseignant(ensRef);
            Examen ex = examGuSystem.trouverExamenParRef(code);
            if (ex == null || !ens.getId().equals(ex.getCreateurId())) {
                return ResponseEntity.badRequest().body("Examen introuvable ou non créé par cet enseignant.");
            }

            List<QuestionDto> dtos = ex.getQuestions().stream()
                    .map(q -> new QuestionDto(
                            q.getId(),
                            q.getEnonce(),
                            q.getType(),
                            q.getBareme()
                    ))
                    .toList();

            return ResponseEntity.ok(dtos);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping("/examens/{code}/questions")
    public ResponseEntity<?> ajouterQuestion(
            @RequestHeader("X-User-Id") String ensRef,
            @PathVariable String code,
            @RequestBody CreerQuestionDto dto
    ) {
        try {
            Enseignant ens = requireEnseignant(ensRef);
            Examen ex = examGuSystem.trouverExamenParRef(code);
            if (ex == null || !ens.getId().equals(ex.getCreateurId())) {
                return ResponseEntity.badRequest().body("Examen introuvable ou non créé par cet enseignant.");
            }

            String enonce = Optional.ofNullable(dto.enonce()).orElse("").trim();
            if (enonce.isEmpty()) {
                return ResponseEntity.badRequest().body("Énoncé obligatoire.");
            }

            double bareme = dto.bareme();
            if (bareme <= 0) {
                return ResponseEntity.badRequest().body("Barème doit être > 0.");
            }

            String typeStr = Optional.ofNullable(dto.type())
                    .orElse("QCM")
                    .trim()
                    .toUpperCase(Locale.ROOT);

            TypeQuestion typeEnum = switch (typeStr) {
                case "COURTE", "LONGUE" -> TypeQuestion.COURTE;
                case "VRAI_FAUX"        -> TypeQuestion.VRAI_FAUX;
                case "QCM"              -> TypeQuestion.QCM;
                default                 -> TypeQuestion.QCM;
            };

            Question q = new Question(enonce, typeEnum, bareme);

            if (typeEnum == TypeQuestion.QCM || typeEnum == TypeQuestion.VRAI_FAUX) {
                List<String> choix = Optional.ofNullable(dto.choix()).orElse(List.of());
                List<Integer> indicesCorrects = Optional.ofNullable(dto.indicesCorrects()).orElse(List.of());

                int indexVisible = 1;
                for (String c : choix) {
                    String lib = Optional.ofNullable(c).orElse("").trim();
                    if (lib.isEmpty()) continue;

                    boolean correct = indicesCorrects.contains(indexVisible);

                    ReponsePossible rp = new ReponsePossible(lib, correct);
                    q.ajouterReponsePossible(rp);

                    indexVisible++;
                }

                if (q.getReponsesPossibles().size() < 2) {
                    return ResponseEntity.badRequest().body("Un QCM/VRAI_FAUX doit avoir au moins 2 choix valides.");
                }
            }

            examGuSystem.ajouterQuestion(ens, ex.getId(), q);

            return ResponseEntity.ok(new QuestionDto(
                    q.getId(),
                    q.getEnonce(),
                    q.getType(),
                    q.getBareme()
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/examens/{code}/ouverture")
    public ResponseEntity<?> ouvrirExamen(
            @RequestHeader("X-User-Id") String ensRef,
            @PathVariable String code,
            @RequestBody OuvrirExamenDto dto
    ) {
        try {
            Enseignant ens = requireEnseignant(ensRef);
            Examen ex = examGuSystem.trouverExamenParRef(code);
            if (ex == null || !ens.getId().equals(ex.getCreateurId())) {
                return ResponseEntity.badRequest().body("Examen introuvable ou non créé par cet enseignant.");
            }

            int duree = dto.dureeMinutes();
            int fenetre = dto.fenetreMinutes();
            if (duree <= 0 || fenetre <= 0) {
                return ResponseEntity.badRequest().body("Durée / fenêtre invalides.");
            }

            LocalDateTime debut;
            if (dto.dateDebut() != null && !dto.dateDebut().isBlank()) {
                debut = LocalDateTime.parse(dto.dateDebut());
            } else {
                debut = LocalDateTime.now();
            }
            LocalDateTime fin = debut.plusMinutes(fenetre);

            examGuSystem.planifierEtOuvrirExamen(ens, ex.getId(), debut, fin, duree);
            return ResponseEntity.ok("Examen ouvert.");
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/examens/{code}/publierNotes")
    public ResponseEntity<?> publierNotes(
            @RequestHeader("X-User-Id") String ensRef,
            @PathVariable String code
    ) {
        try {
            Enseignant ens = requireEnseignant(ensRef);
            Examen ex = examGuSystem.trouverExamenParRef(code);
            if (ex == null || !ens.getId().equals(ex.getCreateurId())) {
                return ResponseEntity.badRequest().body("Examen introuvable ou non créé par cet enseignant.");
            }

            examGuSystem.publierNotes(ens, ex.getId(), LocalDateTime.now());
            return ResponseEntity.ok("Notes publiées pour " + ex.getCode());

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/examens/{code}/fermer")
    public ResponseEntity<?> fermerExamen(
            @RequestHeader("X-User-Id") String ensRef,
            @PathVariable String code
    ) {
        try {
            Enseignant ens = requireEnseignant(ensRef);
            Examen ex = examGuSystem.trouverExamenParRef(code);
            if (ex == null || !ens.getId().equals(ex.getCreateurId())) {
                return ResponseEntity.badRequest().body("Examen introuvable ou non créé par cet enseignant.");
            }

            ex.fermer();
            return ResponseEntity.ok("Examen " + ex.getCode() + " fermé.");

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    // =========================================================================
    //  COPIES : liste, détail, correction, commentaires
    // =========================================================================

    @GetMapping("/examens/{code}/copies")
    public ResponseEntity<?> listerCopies(
            @RequestHeader("X-User-Id") String ensRef,
            @PathVariable String code
    ) {
        try {
            Enseignant ens = requireEnseignant(ensRef);
            Examen ex = examGuSystem.trouverExamenParRef(code);
            if (ex == null || !ens.getId().equals(ex.getCreateurId())) {
                return ResponseEntity.badRequest().body("Examen introuvable ou non créé par cet enseignant.");
            }

            List<Tentative> tentatives = examGuSystem.listerTentativesParExamen(ex.getId());

            List<CopieResumeDto> dtos = tentatives.stream()
                    .map(t -> {
                        double total = t.getScore();          // total auto + manuel
                        double auto = t.calculerScoreAuto();  // seulement auto
                        String etuNom = Optional.ofNullable(t.getEtudiant().getNom()).orElse("");
                        String etuPrenom = Optional.ofNullable(t.getEtudiant().getPrenom()).orElse("");
                        String nomComplet = (etuPrenom + " " + etuNom).trim();

                        boolean soumise = (t.getFin() != null);

                        return new CopieResumeDto(
                                t.getId(),
                                t.getEtudiant().getCode(),
                                nomComplet,
                                auto,
                                total,
                                soumise,
                                t.getDebut(),
                                t.getFin()
                        );
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping("/tentatives/{tentativeId}")
    public ResponseEntity<?> detailCopie(
            @RequestHeader("X-User-Id") String ensRef,
            @PathVariable UUID tentativeId
    ) {
        try {
            Enseignant ens = requireEnseignant(ensRef);
            Tentative t = examGuSystem.trouverTentative(tentativeId);
            if (t == null) {
                return ResponseEntity.badRequest().body("Tentative introuvable.");
            }

            Examen ex = t.getExamen();
            if (ex == null || !ens.getId().equals(ex.getCreateurId())) {
                return ResponseEntity.status(403).body("Accès refusé à cette copie.");
            }

            double total = t.getScore();
            double auto = t.calculerScoreAuto();

            List<QuestionCopieDto> questions = new ArrayList<>();
            for (Question q : ex.getQuestions()) {
                String rep = reponseEtudiantTexte(t, q.getId());
                if (rep == null) rep = "";

                Double noteManuelle = t.getNoteManuelle(q.getId());
                String commentaireManuel = t.getCommentaireManuel(q.getId());

                questions.add(new QuestionCopieDto(
                        q.getId(),
                        q.getEnonce(),
                        q.getType(),
                        q.getBareme(),
                        rep,
                        noteManuelle,
                        commentaireManuel
                ));
            }

            List<CommentaireCopie> commentaires = commentairesParTentative
                    .getOrDefault(t.getId(), List.of());

            List<CommentaireDto> commentairesDto = commentaires.stream()
                    .map(c -> new CommentaireDto(
                            c.getId(),
                            c.getDate(),
                            c.getAuteurCode(),
                            c.getAuteurRole(),
                            c.getMessage()
                    ))
                    .toList();

            String etuNom = Optional.ofNullable(t.getEtudiant().getNom()).orElse("");
            String etuPrenom = Optional.ofNullable(t.getEtudiant().getPrenom()).orElse("");
            String nomComplet = (etuPrenom + " " + etuNom).trim();

            CopieDetailDto dto = new CopieDetailDto(
                    t.getId(),
                    ex.getId(),
                    ex.getCode(),
                    ex.getTitre(),
                    t.getEtudiant().getCode(),
                    nomComplet,
                    total,
                    auto,
                    questions,
                    commentairesDto
            );

            return ResponseEntity.ok(dto);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    /**
     * Correction manuelle d'une question (surtout pour les questions COURTES).
     */
    @PutMapping("/tentatives/{tentativeId}/questions/{questionId}/notation")
    public ResponseEntity<?> corrigerQuestion(
            @RequestHeader("X-User-Id") String ensRef,
            @PathVariable UUID tentativeId,
            @PathVariable UUID questionId,
            @RequestBody CorrectionManuelleDto dto
    ) {
        try {
            Enseignant ens = requireEnseignant(ensRef);

            Tentative t = examGuSystem.trouverTentative(tentativeId);
            if (t == null) {
                return ResponseEntity.badRequest().body("Tentative introuvable.");
            }

            Examen ex = t.getExamen();
            if (ex == null || !ens.getId().equals(ex.getCreateurId())) {
                return ResponseEntity.status(403).body("Accès refusé à cette copie.");
            }

            double note = dto.note();
            String commentaire = Optional.ofNullable(dto.commentaire()).orElse("");
            examGuSystem.corrigerQuestionManuelle(ens, tentativeId, questionId, note, commentaire);

            Question q = ex.trouverQuestion(questionId);
            if (q == null) {
                return ResponseEntity.badRequest().body("Question introuvable.");
            }

            String rep = reponseEtudiantTexte(t, questionId);
            if (rep == null) rep = "";

            Double noteManuelle = t.getNoteManuelle(questionId);
            String commentaireManuel = t.getCommentaireManuel(questionId);

            QuestionCopieDto questionDto = new QuestionCopieDto(
                    q.getId(),
                    q.getEnonce(),
                    q.getType(),
                    q.getBareme(),
                    rep,
                    noteManuelle,
                    commentaireManuel
            );

            double total = t.getScore();
            double auto = t.calculerScoreAuto();

            CorrectionResultatDto resultat = new CorrectionResultatDto(
                    t.getId(),
                    ex.getId(),
                    ex.getCode(),
                    total,
                    auto,
                    questionDto
            );

            return ResponseEntity.ok(resultat);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/tentatives/{tentativeId}/commentaires")
    public ResponseEntity<?> ajouterCommentaire(
            @RequestHeader("X-User-Id") String ensRef,
            @PathVariable UUID tentativeId,
            @RequestBody NouveauCommentaireDto dto
    ) {
        try {
            Enseignant ens = requireEnseignant(ensRef);
            Tentative t = examGuSystem.trouverTentative(tentativeId);
            if (t == null) {
                return ResponseEntity.badRequest().body("Tentative introuvable.");
            }

            Examen ex = t.getExamen();
            if (ex == null || !ens.getId().equals(ex.getCreateurId())) {
                return ResponseEntity.status(403).body("Accès refusé à cette copie.");
            }

            String message = Optional.ofNullable(dto.message()).orElse("").trim();
            if (message.isEmpty()) {
                return ResponseEntity.badRequest().body("Message de commentaire vide.");
            }

            CommentaireCopie c = new CommentaireCopie(
                    UUID.randomUUID(),
                    tentativeId,
                    LocalDateTime.now(),
                    ensRef,
                    Role.ENSEIGNANT,
                    message
            );

            commentairesParTentative
                    .computeIfAbsent(tentativeId, k -> new ArrayList<>())
                    .add(c);

            CommentaireDto retour = new CommentaireDto(
                    c.getId(),
                    c.getDate(),
                    c.getAuteurCode(),
                    c.getAuteurRole(),
                    c.getMessage()
            );

            return ResponseEntity.ok(retour);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping("/tentatives/{tentativeId}/commentaires")
    public ResponseEntity<?> listerCommentaires(
            @RequestHeader("X-User-Id") String ensRef,
            @PathVariable UUID tentativeId
    ) {
        try {
            Enseignant ens = requireEnseignant(ensRef);
            Tentative t = examGuSystem.trouverTentative(tentativeId);
            if (t == null) {
                return ResponseEntity.badRequest().body("Tentative introuvable.");
            }

            Examen ex = t.getExamen();
            if (ex == null || !ens.getId().equals(ex.getCreateurId())) {
                return ResponseEntity.status(403).body("Accès refusé à cette copie.");
            }

            List<CommentaireDto> dtos = commentairesParTentative
                    .getOrDefault(tentativeId, List.of())
                    .stream()
                    .map(c -> new CommentaireDto(
                            c.getId(),
                            c.getDate(),
                            c.getAuteurCode(),
                            c.getAuteurRole(),
                            c.getMessage()
                    ))
                    .toList();

            return ResponseEntity.ok(dtos);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    // =========================================================================
    //  Helpers internes
    // =========================================================================

    private static String reponseEtudiantTexte(Tentative t, UUID questionId) {
        for (ReponseDonnee rd : t.getReponses()) {
            if (rd != null && rd.getQuestion() != null
                    && questionId.equals(rd.getQuestion().getId())) {
                return rd.getContenu();
            }
        }
        return null;
    }

    // =========================================================================
    //  DTO internes au contrôleur
    // =========================================================================

    public record ExamenDto(
            UUID id,
            String code,
            String titre,
            EtatExamen etat,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            int dureeMinutes
    ) {}

    public record CreerExamenDto(
            String titre
    ) {}

    public record QuestionDto(
            UUID id,
            String enonce,
            TypeQuestion type,
            double bareme
    ) {}

    public record CreerQuestionDto(
            String enonce,
            double bareme,
            String type,
            List<String> choix,
            List<Integer> indicesCorrects,
            Boolean bonneReponseVraiFaux
    ) {}

    public record OuvrirExamenDto(
            int dureeMinutes,
            int fenetreMinutes,
            String dateDebut
    ) {}

    public record CopieResumeDto(
            UUID tentativeId,
            String etudiantCode,
            String etudiantNom,
            double noteAuto,
            double noteTotale,
            boolean soumise,
            LocalDateTime debut,
            LocalDateTime fin
    ) {}

    public record QuestionCopieDto(
            UUID questionId,
            String enonce,
            TypeQuestion type,
            double bareme,
            String reponse,
            Double noteManuelle,
            String commentaireManuel
    ) {}

    public record CommentaireDto(
            UUID id,
            LocalDateTime date,
            String auteurCode,
            Role auteurRole,
            String message
    ) {}

    public record CopieDetailDto(
            UUID tentativeId,
            UUID examenId,
            String examenCode,
            String examenTitre,
            String etudiantCode,
            String etudiantNom,
            double noteTotale,
            double noteAuto,
            List<QuestionCopieDto> questions,
            List<CommentaireDto> commentaires
    ) {}

    public record CorrectionManuelleDto(
            double note,
            String commentaire
    ) {}

    public record CorrectionResultatDto(
            UUID tentativeId,
            UUID examenId,
            String examenCode,
            double noteTotale,
            double noteAuto,
            QuestionCopieDto question
    ) {}

    public record NouveauCommentaireDto(
            String message
    ) {}
}
