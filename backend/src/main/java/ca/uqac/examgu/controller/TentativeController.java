package ca.uqac.examgu.controller;

import ca.uqac.examgu.model.*;
import ca.uqac.examgu.model.Enumerations.StatutTentative;
import ca.uqac.examgu.repository.EnseignantRepository;
import ca.uqac.examgu.repository.EtudiantRepository;
import ca.uqac.examgu.repository.UtilisateurRepository;
import ca.uqac.examgu.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/tentatives")
public class TentativeController {

    private final TentativeService tentativeService;
    private final EtudiantService etudiantService;

    public TentativeController(TentativeService tentativeService, EtudiantService etudiantService) {
        this.tentativeService = tentativeService;
        this.etudiantService = etudiantService;
    }


    @PostMapping("/{tentativeId}/save-reponse")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> sauvegarderReponse(
            @PathVariable UUID tentativeId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        try {
            UUID etudiantId = getEtudiantCourantId(authentication);
            UUID questionId = UUID.fromString((String) request.get("questionId"));
            String contenu = (String) request.get("contenu");

            Tentative tentative = tentativeService.sauvegarderReponse(tentativeId,
                    questionId, contenu, etudiantId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Réponse sauvegardée avec succès");
            response.put("tentative", tentative);
            response.put("tempsRestant", tentative.tempsRestant());

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    @GetMapping("/{tentativeId}/questions")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> getQuestionsPourTentative(
            @PathVariable UUID tentativeId,
            Authentication authentication) {

        try {
            UUID etudiantId = getEtudiantCourantId(authentication);

            // Récupérer la tentative pour vérifier les droits
            Tentative tentative = tentativeService.getTentative(tentativeId, etudiantId);

            // Vérifier que la tentative est en cours
            if (!tentative.getStatut().equals(StatutTentative.EN_COURS)) {
                return ResponseEntity.badRequest()
                        .body("La tentative n'est pas en cours. Statut: " + tentative.getStatut());
            }

            Examen examen = tentative.getExamen();
            List<Map<String, Object>> questionsDTO = new ArrayList<>();

            // Pour chaque question de l'examen
            for (int i = 0; i < examen.getQuestions().size(); i++) {
                Question question = examen.getQuestions().get(i);
                Map<String, Object> questionDTO = new HashMap<>();

                // Informations de base
                questionDTO.put("id", question.getId());
                questionDTO.put("numero", i + 1);
                questionDTO.put("enonce", question.getEnonce());
                questionDTO.put("bareme", question.getBareme());
                questionDTO.put("type", question.getClass().getSimpleName());

                // Récupérer la réponse déjà donnée (si elle existe)
                ReponseDonnee reponseExistante = tentative.getReponsePourQuestion(question.getId());
                if (reponseExistante != null) {
                    questionDTO.put("reponseExistante", reponseExistante.getContenu());
                    questionDTO.put("dateMajReponse", reponseExistante.getDateMaj());
                } else {
                    questionDTO.put("reponseExistante", null);
                }

                // Traitement spécifique selon le type de question
                if (question instanceof QuestionAChoix) {
                    QuestionAChoix questionChoix = (QuestionAChoix) question;
                    questionDTO.put("typeQuestion", "CHOIX");
                    questionDTO.put("typeChoix", questionChoix.getTypeChoix());
                    questionDTO.put("nombreChoixMin", questionChoix.getNombreChoixMin());
                    questionDTO.put("nombreChoixMax", questionChoix.getNombreChoixMax());

                    // Pour les questions à choix multiples (QCM)
                    if (questionChoix.getTypeChoix() == QuestionAChoix.TypeChoix.QCM) {
                        questionDTO.put("politiqueCorrection", questionChoix.getPolitiqueCorrectionQCM());
                    }

                    // Liste des choix possibles (sans indiquer les bonnes réponses)
                    List<Map<String, Object>> choixDTO = new ArrayList<>();
                    for (ReponsePossible choix : questionChoix.getReponsesPossibles()) {
                        Map<String, Object> choixMap = new HashMap<>();
                        choixMap.put("id", choix.getId());
                        choixMap.put("texte", choix.getLibelle());
                        //choixMap.put("ordre", choix.getOrdre());
                        // NOTE: On ne met PAS "correcte" pour ne pas donner la réponse
                        choixDTO.add(choixMap);
                    }
                    questionDTO.put("choix", choixDTO);

                } else if (question instanceof QuestionADeveloppement) {
                    QuestionADeveloppement questionDev = (QuestionADeveloppement) question;
                    questionDTO.put("typeQuestion", "DEVELOPPEMENT");
                }

                questionsDTO.add(questionDTO);
            }

            // Retourner également les informations de la tentative
            Map<String, Object> response = new HashMap<>();
            response.put("tentative", mapTentativeInfo(tentative));
            response.put("questions", questionsDTO);
            response.put("examen", mapExamenInfo(examen));

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Erreur lors de la récupération des questions: " + e.getMessage());
        }
    }

    @GetMapping("/{tentativeId}/statut-temps-reel")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> getStatutTempsReel(
            @PathVariable UUID tentativeId,
            Authentication authentication) {

        try {
            UUID etudiantId = getEtudiantCourantId(authentication);

            // Récupérer la tentative
            Tentative tentative = tentativeService.getTentative(tentativeId, etudiantId);

            // Calculer les statistiques
            Map<String, Object> statut = new HashMap<>();

            // 1. Informations temporelles
            statut.put("tempsRestantSecondes", tentative.tempsRestant());
            statut.put("tempsRestantMinutes", tentative.tempsRestant() / 60);
            statut.put("debut", tentative.getDebut());
            statut.put("finPrevue", tentative.getFin());
            statut.put("dateSoumission", tentative.getDateSoumission());

            // 2. État de la tentative
            statut.put("statut", tentative.getStatut());
            statut.put("estExpiree", tentative.estExpiree());
            statut.put("estEnCours", tentative.getStatut() == StatutTentative.EN_COURS);
            statut.put("estSoumise", tentative.getStatut() == StatutTentative.SOUMISE);

            // 3. Progression
            statut.put("nombreQuestionsRepondues", tentative.getNombreQuestionsRepondues());
            statut.put("nombreQuestionsTotal", tentative.getExamen().getQuestions().size());
            statut.put("pourcentageCompletion", tentative.getPourcentageCompletion());

            // 4. Score (si disponible)
            statut.put("scoreActuel", tentative.getScore());
            statut.put("estCorrigee", tentative.estCorrigee());

            // 5. Dernière activité
            statut.put("dateDerniereModification", tentative.getDateModification());

            // 6. Calculer le temps écoulé
            LocalDateTime maintenant = LocalDateTime.now();
            if (tentative.getDebut() != null) {
                long secondesEcoulees = Duration.between(tentative.getDebut(), maintenant).getSeconds();
                statut.put("tempsEcouleSecondes", secondesEcoulees);
                statut.put("tempsEcouleMinutes", secondesEcoulees / 60);

                // Pourcentage du temps écoulé
                if (tentative.getExamen().getDureeMinutes() > 0) {
                    double pourcentageTempsEcoule = (double) secondesEcoulees /
                            (tentative.getExamen().getDureeMinutes() * 60) * 100;
                    statut.put("pourcentageTempsEcoule", Math.min(100, pourcentageTempsEcoule));
                }
            }

            // 7. Alertes et avertissements
            List<String> alertes = new ArrayList<>();

            if (tentative.estExpiree()) {
                alertes.add("Le temps est écoulé! La tentative sera soumise automatiquement.");
            } else if (tentative.tempsRestant() < 300) { // 5 minutes
                alertes.add("Attention! Il reste moins de 5 minutes.");
            } else if (tentative.tempsRestant() < 600) { // 10 minutes
                alertes.add("Il reste moins de 10 minutes.");
            }

            if (tentative.getPourcentageCompletion() < 50 &&
                    tentative.tempsRestant() < 1800) { // 30 minutes
                alertes.add("Attention: moins de 50% de complétion avec peu de temps restant.");
            }

            statut.put("alertes", alertes);

            // 8. Recommandations
            List<String> recommandations = new ArrayList<>();

            long tempsRestant = tentative.tempsRestant();
            int questionsRestantes = tentative.getExamen().getQuestions().size() -
                    tentative.getNombreQuestionsRepondues();

            if (questionsRestantes > 0 && tempsRestant > 0) {
                long tempsParQuestion = tempsRestant / questionsRestantes;
                recommandations.add("Temps moyen par question restante: " +
                        tempsParQuestion + " secondes");

                if (tempsParQuestion < 60) {
                    recommandations.add("Conseil: Concentrez-vous sur les questions non répondues");
                }
            }

            statut.put("recommandations", recommandations);

            // 9. Métriques de performance
            if (tentative.getDebut() != null) {
                long dureeTotale = Duration.between(tentative.getDebut(), maintenant).getSeconds();
                if (dureeTotale > 0) {
                    double questionsParMinute = (double) tentative.getNombreQuestionsRepondues() /
                            (dureeTotale / 60.0);
                    statut.put("vitesseQuestionsParMinute", String.format("%.1f", questionsParMinute));
                }
            }

            // 10. Résumé par type de question (si les réponses existent)
            if (!tentative.getReponses().isEmpty()) {
                Map<String, Object> statsQuestions = new HashMap<>();

                long qcmRepondues = tentative.getReponses().stream()
                        .filter(r -> r.getQuestion() instanceof QuestionAChoix)
                        .filter(r -> r.getContenu() != null && !r.getContenu().trim().isEmpty())
                        .count();

                long devRepondues = tentative.getReponses().stream()
                        .filter(r -> r.getQuestion() instanceof QuestionADeveloppement)
                        .filter(r -> r.getContenu() != null && !r.getContenu().trim().isEmpty())
                        .count();

                statsQuestions.put("qcmRepondues", qcmRepondues);
                statsQuestions.put("devRepondues", devRepondues);

                // Compter le total par type dans l'examen
                long totalQCM = tentative.getExamen().getQuestions().stream()
                        .filter(q -> q instanceof QuestionAChoix)
                        .count();

                long totalDev = tentative.getExamen().getQuestions().stream()
                        .filter(q -> q instanceof QuestionADeveloppement)
                        .count();

                statsQuestions.put("totalQCM", totalQCM);
                statsQuestions.put("totalDev", totalDev);

                if (totalQCM > 0) {
                    statsQuestions.put("pourcentageQCMRepondues", (qcmRepondues * 100.0) / totalQCM);
                }

                if (totalDev > 0) {
                    statsQuestions.put("pourcentageDevRepondues", (devRepondues * 100.0) / totalDev);
                }

                statut.put("statistiquesQuestions", statsQuestions);
            }

            return ResponseEntity.ok(statut);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Erreur lors de la récupération du statut: " + e.getMessage());
        }
    }

    // Ajoutez cette méthode pour un endpoint de vérification rapide
    @GetMapping("/{tentativeId}/verification-rapide")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> getVerificationRapide(
            @PathVariable UUID tentativeId,
            Authentication authentication) {

        try {
            UUID etudiantId = getEtudiantCourantId(authentication);
            Tentative tentative = tentativeService.getTentative(tentativeId, etudiantId);

            Map<String, Object> verification = new HashMap<>();

            // Informations essentielles seulement
            verification.put("statut", tentative.getStatut().toString());
            verification.put("tempsRestant", tentative.tempsRestant());
            verification.put("estExpiree", tentative.estExpiree());
            verification.put("questionsRepondues", tentative.getNombreQuestionsRepondues());
            verification.put("questionsTotal", tentative.getExamen().getQuestions().size());
            verification.put("derniereActivite", tentative.getDateModification());

            return ResponseEntity.ok(verification);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Erreur de vérification: " + e.getMessage());
        }
    }

    // Méthode utilitaire pour mapper les infos de la tentative
    private Map<String, Object> mapTentativeInfo(Tentative tentative) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", tentative.getId());
        map.put("debut", tentative.getDebut());
        map.put("statut", tentative.getStatut());
        map.put("tempsRestant", tentative.tempsRestant());
        map.put("estExpiree", tentative.estExpiree());
        map.put("nombreQuestionsRepondues", tentative.getNombreQuestionsRepondues());
        map.put("nombreQuestionsTotal", tentative.getExamen().getQuestions().size());
        map.put("pourcentageCompletion", tentative.getPourcentageCompletion());
        return map;
    }

    // Méthode utilitaire pour mapper les infos de l'examen
    private Map<String, Object> mapExamenInfo(Examen examen) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", examen.getId());
        map.put("titre", examen.getTitre());
        map.put("description", examen.getDescription());
        map.put("dureeMinutes", examen.getDureeMinutes());
        map.put("dateDebut", examen.getDateDebut());
        map.put("dateFin", examen.getDateFin());
        map.put("barèmeTotal", examen.totalPoints());
        return map;
    }


    @PostMapping("/{tentativeId}/soumettre")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> soumettreTentative(@PathVariable UUID tentativeId, Authentication authentication) {
        try {
            UUID etudiantId = getEtudiantCourantId(authentication);

            Tentative tentative = tentativeService.soumettreTentative(tentativeId, etudiantId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Tentative soumise avec succès");
            response.put("tentative", tentative);
            response.put("score", tentative.getScore());

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la soumission: " + e.getMessage());
        }
    }


    @GetMapping("/{tentativeId}/reprendre")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> reprendreTentative(@PathVariable UUID tentativeId, Authentication authentication) {
        try {
            UUID etudiantId = getEtudiantCourantId(authentication);

            Tentative tentative = tentativeService.reprendreTentative(tentativeId, etudiantId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Tentative reprise avec succès");
            response.put("tentative", tentative);
            response.put("tempsRestant", tentative.tempsRestant());
            response.put("reponses", tentative.getReponses());

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la reprise: " + e.getMessage());
        }
    }


    @GetMapping("/{tentativeId}")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> getTentative(@PathVariable UUID tentativeId, Authentication authentication) {
        try {
            String email = authentication.getName();
            Etudiant etudiant = etudiantService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Etudiant non trouvé"));
            UUID etudiantId = etudiant.getId();

            Tentative tentative = tentativeService.getTentative(tentativeId, etudiantId);

            return ResponseEntity.ok(tentative);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération: " + e.getMessage());
        }
    }


    @GetMapping("/{tentativeId}/temps-restant")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> getTempsRestant(@PathVariable UUID tentativeId, Authentication authentication) {
        try {
            UUID etudiantId = getEtudiantCourantId(authentication);

            Tentative tentative = tentativeService.getTentative(tentativeId, etudiantId);

            Map<String, Object> response = new HashMap<>();
            response.put("tempsRestant", tentative.tempsRestant());
            response.put("statut", tentative.getStatut());
            response.put("expiree", tentative.estExpiree());

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération: " + e.getMessage());
        }
    }


    private UUID getEtudiantCourantId(Authentication authentication) {
        String email = authentication.getName();
        Etudiant etudiant = etudiantService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Etudiant non trouvé"));
        return etudiant.getId();
    }
}