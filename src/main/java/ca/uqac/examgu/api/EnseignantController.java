package ca.uqac.examgu.api;

import ca.uqac.examgu.application.ExamGuSystem;
import ca.uqac.examgu.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/enseignants")
public class EnseignantController {

    private final ExamGuSystem examGuSystem;

    public EnseignantController(ExamGuSystem examGuSystem) {
        this.examGuSystem = examGuSystem;
    }

    /**
     * Création d'un examen par un enseignant.
     */
    @PostMapping("/examens")
    public ResponseEntity<?> creerExamen(
            @RequestHeader("X-User-Id") String ensRef,
            @RequestBody CreerExamenDto dto
    ) {
        Utilisateur u = examGuSystem.trouverUtilisateurParRef(ensRef);
        if (!(u instanceof Enseignant ens)) {
            return ResponseEntity.status(403).body("Accès refusé : ENSEIGNANT requis.");
        }

        try {
            Examen ex = examGuSystem.creerExamen(ens, dto.titre());
            return ResponseEntity.ok(
                    new ExamenDto(
                            ex.getId(),
                            ex.getCode(),
                            ex.getEtat(),
                            ex.getTitre()
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Ajout d'une question à un examen.
     */
    @PostMapping("/examens/{examenRef}/questions")
    public ResponseEntity<?> ajouterQuestion(
            @RequestHeader("X-User-Id") String ensRef,
            @PathVariable String examenRef,
            @RequestBody CreerQuestionDto dto
    ) {
        Utilisateur u = examGuSystem.trouverUtilisateurParRef(ensRef);
        if (!(u instanceof Enseignant ens)) {
            return ResponseEntity.status(403).body("Accès refusé : ENSEIGNANT requis.");
        }

        Examen ex = examGuSystem.trouverExamenParRef(examenRef);
        if (ex == null) {
            return ResponseEntity.badRequest().body("Examen introuvable : " + examenRef);
        }

        Question q = new Question(dto.enonce(), dto.type(), dto.bareme());

        if (dto.type() == TypeQuestion.QCM) {
            if (dto.choix() == null || dto.choix().isEmpty()) {
                return ResponseEntity.badRequest().body("Choix obligatoires pour un QCM.");
            }
            Set<Integer> correctIdx = new HashSet<>(dto.indicesCorrects());
            for (int i = 0; i < dto.choix().size(); i++) {
                boolean ok = correctIdx.contains(i + 1); // indices 1..n
                q.ajouterReponsePossible(new ReponsePossible(dto.choix().get(i), ok));
            }
        } else if (dto.type() == TypeQuestion.VRAI_FAUX) {
            if (dto.bonneReponseVraiFaux() == null) {
                return ResponseEntity.badRequest()
                        .body("Bonne réponse (vrai/faux) requise pour VRAI_FAUX.");
            }
            boolean vraiCorrect = dto.bonneReponseVraiFaux();
            q.ajouterReponsePossible(new ReponsePossible("vrai", vraiCorrect));
            q.ajouterReponsePossible(new ReponsePossible("faux", !vraiCorrect));
        } else if (dto.type() == TypeQuestion.COURTE) {
            // rien à ajouter, correction manuelle
        }

        try {
            examGuSystem.ajouterQuestion(ens, ex.getId(), q);
            return ResponseEntity.ok(
                    new QuestionDto(
                            q.getId(),
                            q.getEnonce(),
                            q.getType(),
                            q.getBareme()
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Planifier + ouvrir un examen (fenêtre et durée).
     */
    @PostMapping("/examens/{examenRef}/ouverture")
    public ResponseEntity<?> ouvrirExamen(
            @RequestHeader("X-User-Id") String ensRef,
            @PathVariable String examenRef,
            @RequestBody OuvrirExamenDto dto
    ) {
        Utilisateur u = examGuSystem.trouverUtilisateurParRef(ensRef);
        if (!(u instanceof Enseignant ens)) {
            return ResponseEntity.status(403).body("Accès refusé : ENSEIGNANT requis.");
        }

        Examen ex = examGuSystem.trouverExamenParRef(examenRef);
        if (ex == null) {
            return ResponseEntity.badRequest().body("Examen introuvable : " + examenRef);
        }

        LocalDateTime debut = LocalDateTime.now();
        LocalDateTime fin = debut.plusMinutes(dto.fenetreMinutes());

        try {
            examGuSystem.planifierEtOuvrirExamen(ens, ex.getId(), debut, fin, dto.dureeMinutes());
            return ResponseEntity.ok("Examen planifié + ouvert : " + ex.getCode());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Publication des notes d'un examen.
     */
    @PostMapping("/examens/{examenRef}/publier-notes")
    public ResponseEntity<?> publierNotes(
            @RequestHeader("X-User-Id") String ensRef,
            @PathVariable String examenRef
    ) {
        Utilisateur u = examGuSystem.trouverUtilisateurParRef(ensRef);
        if (!(u instanceof Enseignant ens)) {
            return ResponseEntity.status(403).body("Accès refusé : ENSEIGNANT requis.");
        }

        Examen ex = examGuSystem.trouverExamenParRef(examenRef);
        if (ex == null) {
            return ResponseEntity.badRequest().body("Examen introuvable : " + examenRef);
        }

        try {
            examGuSystem.publierNotes(ens, ex.getId(), LocalDateTime.now());
            return ResponseEntity.ok("Notes publiées pour " + ex.getCode());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    /**
     * Liste des examens créés par un enseignant.
     * Appelé par le bouton "Charger mes examens" dans la vue Enseignant.
     */
    @GetMapping("/examens")
    public ResponseEntity<?> listerExamensEnseignant(
            @RequestHeader("X-User-Id") String ensRef
    ) {
        // On retrouve l'utilisateur à partir de ENS-0001, etc.
        Utilisateur u = examGuSystem.trouverUtilisateurParRef(ensRef);
        if (!(u instanceof Enseignant ens)) {
            return ResponseEntity.status(403).body("Accès refusé : ENSEIGNANT requis.");
        }

        // On récupère les examens créés par cet enseignant
        List<Examen> examens = examGuSystem.listerExamensPourEnseignant(ens);

        // On les convertit en DTO pour le frontend
        List<ExamenDto> dtos = examens.stream()
                .map(ex -> new ExamenDto(
                        ex.getId(),
                        ex.getCode(),
                        ex.getEtat(),
                        ex.getTitre()
                ))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // ===== DTO =====


    public record CreerExamenDto(String titre) {}

    public record ExamenDto(
            UUID id,
            String code,
            EtatExamen etat,
            String titre
    ) {}

    public record CreerQuestionDto(
            String enonce,
            double bareme,
            TypeQuestion type,
            List<String> choix,
            List<Integer> indicesCorrects,
            Boolean bonneReponseVraiFaux
    ) {}

    public record QuestionDto(
            UUID id,
            String enonce,
            TypeQuestion type,
            double bareme
    ) {}

    public record OuvrirExamenDto(
            int dureeMinutes,
            int fenetreMinutes
    ) {}
}
