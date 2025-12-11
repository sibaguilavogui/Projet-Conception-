package ca.uqac.examgu.controller;

import ca.uqac.examgu.model.*;
import ca.uqac.examgu.model.Enumerations.StatutTentative;
import ca.uqac.examgu.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tentatives")
public class TentativeController {

    private final TentativeService tentativeService;
    private final EtudiantService etudiantService;
    private final EnseignantService enseignantService;

    public TentativeController(TentativeService tentativeService, EtudiantService etudiantService, EnseignantService enseignantService) {
        this.tentativeService = tentativeService;
        this.etudiantService = etudiantService;
        this.enseignantService = enseignantService;
    }


    @PostMapping("/{tentativeId}/save-reponse")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> sauvegarderReponse(@PathVariable UUID tentativeId, @RequestBody Map<String, Object> request,
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
    public ResponseEntity<?> getQuestionsPourTentative(@PathVariable UUID tentativeId, Authentication authentication) {

        try {
            UUID etudiantId = getEtudiantCourantId(authentication);

            Tentative tentative = tentativeService.getTentative(tentativeId, etudiantId);

            if (!tentative.getStatut().equals(StatutTentative.EN_COURS)) {
                return ResponseEntity.badRequest()
                        .body("La tentative n'est pas en cours. Statut: " + tentative.getStatut());
            }

            Examen examen = tentative.getExamen();
            List<Map<String, Object>> questionsDTO = new ArrayList<>();

            for (int i = 0; i < examen.getQuestions().size(); i++) {
                Question question = examen.getQuestions().get(i);
                Map<String, Object> questionDTO = new HashMap<>();

                questionDTO.put("id", question.getId());
                questionDTO.put("numero", i + 1);
                questionDTO.put("enonce", question.getEnonce());
                questionDTO.put("bareme", question.getBareme());
                questionDTO.put("type", question.getClass().getSimpleName());

                ReponseDonnee reponseExistante = tentative.trouverReponse(question.getId());
                if (reponseExistante != null) {
                    questionDTO.put("reponseExistante", reponseExistante.getContenu());
                    questionDTO.put("dateMajReponse", reponseExistante.getDateMaj());
                } else {
                    questionDTO.put("reponseExistante", null);
                }

                if (question instanceof QuestionAChoix) {
                    QuestionAChoix questionChoix = (QuestionAChoix) question;
                    questionDTO.put("typeQuestion", "CHOIX");
                    questionDTO.put("typeChoix", questionChoix.getTypeChoix());

                    if (questionChoix.getTypeChoix() == QuestionAChoix.TypeChoix.QCM) {
                        questionDTO.put("politiqueCorrection", questionChoix.getPolitiqueCorrectionQCM());
                    }

                    List<Map<String, Object>> choixDTO = new ArrayList<>();
                    for (ReponsePossible choix : questionChoix.getReponsesPossibles()) {
                        Map<String, Object> choixMap = new HashMap<>();
                        choixMap.put("id", choix.getId());
                        choixMap.put("texte", choix.getLibelle());
                        choixDTO.add(choixMap);
                    }
                    questionDTO.put("choix", choixDTO);

                } else if (question instanceof QuestionADeveloppement) {
                    QuestionADeveloppement questionDev = (QuestionADeveloppement) question;
                    questionDTO.put("typeQuestion", "DEVELOPPEMENT");
                }

                questionsDTO.add(questionDTO);
            }

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
    public ResponseEntity<?> getStatutTempsReel(@PathVariable UUID tentativeId, Authentication authentication) {

        try {
            UUID etudiantId = getEtudiantCourantId(authentication);

            Tentative tentative = tentativeService.getTentative(tentativeId, etudiantId);

            Map<String, Object> statut = new HashMap<>();

            statut.put("tempsRestantSecondes", tentative.tempsRestant());
            statut.put("tempsRestantMinutes", tentative.tempsRestant() / 60);
            statut.put("debut", tentative.getDebut());
            statut.put("finPrevue", tentative.getFin());
            statut.put("dateSoumission", tentative.getDateSoumission());

            statut.put("statut", tentative.getStatut());
            statut.put("estExpiree", tentative.estExpiree());
            statut.put("estEnCours", tentative.getStatut() == StatutTentative.EN_COURS);
            statut.put("estSoumise", tentative.getStatut() == StatutTentative.SOUMISE);

            statut.put("nombreQuestionsRepondues", tentative.getNombreQuestionsRepondues());
            statut.put("nombreQuestionsTotal", tentative.getExamen().getQuestions().size());
            statut.put("pourcentageCompletion", tentative.getPourcentageCompletion());

            statut.put("dateDerniereModification", tentative.getDateModification());

            LocalDateTime maintenant = LocalDateTime.now();
            if (tentative.getDebut() != null) {
                long secondesEcoulees = Duration.between(tentative.getDebut(), maintenant).getSeconds();
                statut.put("tempsEcouleSecondes", secondesEcoulees);
                statut.put("tempsEcouleMinutes", secondesEcoulees / 60);

                if (tentative.getExamen().getDureeMinutes() > 0) {
                    double pourcentageTempsEcoule = (double) secondesEcoulees /
                            (tentative.getExamen().getDureeMinutes() * 60) * 100;
                    statut.put("pourcentageTempsEcoule", Math.min(100, pourcentageTempsEcoule));
                }
            }

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

            if (tentative.getDebut() != null) {
                long dureeTotale = Duration.between(tentative.getDebut(), maintenant).getSeconds();
                if (dureeTotale > 0) {
                    double questionsParMinute = (double) tentative.getNombreQuestionsRepondues() /
                            (dureeTotale / 60.0);
                    statut.put("vitesseQuestionsParMinute", String.format("%.1f", questionsParMinute));
                }
            }

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


    @GetMapping("/{tentativeId}/verification-rapide")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> getVerificationRapide(@PathVariable UUID tentativeId, Authentication authentication) {

        try {
            UUID etudiantId = getEtudiantCourantId(authentication);
            Tentative tentative = tentativeService.getTentative(tentativeId, etudiantId);

            Map<String, Object> verification = new HashMap<>();

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
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENSEIGNANT')")
    public ResponseEntity<?> getTentative(@PathVariable UUID tentativeId, Authentication auth) {
        boolean estEnseignant = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ENSEIGNANT"));

        boolean estEtudiant = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ETUDIANT"));

        try {
            if(estEtudiant){
                String email = auth.getName();
                Etudiant etudiant = etudiantService.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Etudiant non trouvé"));
                UUID etudiantId = etudiant.getId();

                Tentative tentative = tentativeService.getTentative(tentativeId, etudiantId);

                return ResponseEntity.ok(tentative);
            }
            if(estEnseignant){
                UUID enseignantId = getEnseignantCourantId(auth);

                Tentative tentative = tentativeService.getTentativePourCorrection(tentativeId, enseignantId);

                return ResponseEntity.ok(formaterTentativePourCorrection(tentative));
            }

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération: " + e.getMessage());
        }
        return ResponseEntity.status(403).body("Accès refusé");
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


    @GetMapping("/{tentativeId}/reponses-developpement")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> getReponsesDeveloppementTentative(@PathVariable UUID tentativeId,
            Authentication auth) {

        try {
            UUID enseignantId = getEnseignantCourantId(auth);

            List<ReponseDonnee> reponsesDev = tentativeService.getReponsesDevTentative(tentativeId, enseignantId);

            List<Map<String, Object>> reponsesADeveloppement = reponsesDev.stream()
                    .map(reponse -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("questionId", reponse.getQuestion().getId());
                        dto.put("contenu", reponse.getContenu());
                        dto.put("reponseId", reponse.getId());
                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(reponsesADeveloppement);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }


    @PostMapping("/{tentativeId}/reponses/{reponseId}/corriger-developpement")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> corrigerQuestionDeveloppement(@PathVariable UUID tentativeId, @PathVariable UUID reponseId,
                                                           @RequestBody Map<String, Object> request, Authentication auth) {

        try {
            UUID enseignantId = getEnseignantCourantId(auth);

            Tentative tentative = tentativeService.getTentativePourCorrection(tentativeId, enseignantId);

            double note = ((Number) request.get("note")).doubleValue();
            String commentaire = (String) request.get("commentaire");

            Tentative tentativeCorrigee = tentativeService.corrigerQuestionDeveloppement(
                    tentativeId, reponseId, note, commentaire, enseignantId);

            return ResponseEntity.ok(tentativeCorrigee);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }


    @GetMapping("/{tentativeId}/questions-developpement")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> getQuestionsDeveloppementPourCorrection(@PathVariable UUID tentativeId, Authentication auth) {

        try {
            UUID enseignantId = getEnseignantCourantId(auth);

            Tentative tentative = tentativeService.getTentativePourCorrection(tentativeId, enseignantId);

            Examen examen = tentative.getExamen();
            if (examen.getDateFin() != null && examen.getDateFin().isAfter(LocalDateTime.now())) {
                return ResponseEntity.badRequest()
                        .body("La correction n'est disponible qu'après la date de fin de l'examen");
            }

            List<Map<String, Object>> questionsDev = new ArrayList<>();
            for (Question question : examen.getQuestions()) {
                if (question instanceof QuestionADeveloppement) {
                    Map<String, Object> questionMap = new HashMap<>();
                    questionMap.put("id", question.getId());
                    questionMap.put("enonce", question.getEnonce());
                    questionMap.put("bareme", question.getBareme());
                    questionMap.put("ordre", examen.getQuestions().indexOf(question) + 1);

                    // Récupérer la réponse de l'étudiant
                    ReponseDonnee reponse = tentative.trouverReponse(question.getId());
                    if (reponse != null) {
                        questionMap.put("reponseId", reponse.getId());
                        questionMap.put("contenu", reponse.getContenu());
                        questionMap.put("notePartielle", reponse.getNotePartielle());
                        questionMap.put("estCorrigee", reponse.isEstCorrigee());
                    }

                    questionsDev.add(questionMap);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("tentativeId", tentative.getId());
            response.put("etudiant", mapEtudiant(tentative.getEtudiant()));
            response.put("questionsDeveloppement", questionsDev);
            response.put("examenTermine", examen.getDateFin().isBefore(LocalDateTime.now()));
            response.put("dateFin", examen.getDateFin());

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Erreur lors de la récupération: " + e.getMessage());
        }
    }

    @PostMapping("/{tentativeId}/corriger-developpement")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> corrigerQuestionsDeveloppement(@PathVariable UUID tentativeId,
            @RequestBody Map<String, Object> corrections, Authentication auth) {

        try {
            UUID enseignantId = getEnseignantCourantId(auth);

            Tentative tentative = tentativeService.getTentativePourCorrection(tentativeId, enseignantId);

            Examen examen = tentative.getExamen();
            if (examen.getDateFin() != null && examen.getDateFin().isAfter(LocalDateTime.now())) {
                return ResponseEntity.badRequest()
                        .body("La correction n'est disponible qu'après la date de fin de l'examen");
            }

            List<Map<String, Object>> correctionsList = (List<Map<String, Object>>) corrections.get("corrections");
            Map<UUID, Double> notes = new HashMap<>();
            Map<UUID, String> commentaires = new HashMap<>();

            for (Map<String, Object> correction : correctionsList) {
                UUID reponseId = UUID.fromString((String) correction.get("reponseId"));
                double note = ((Number) correction.get("note")).doubleValue();
                String commentaire = (String) correction.get("commentaire");

                notes.put(reponseId, note);
                commentaires.put(reponseId, commentaire);
            }

            Tentative tentativeCorrigee = tentativeService.corrigerTentativeComplete(
                    tentativeId, notes, commentaires, enseignantId);

            return ResponseEntity.ok(tentativeCorrigee);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }


    private Map<String, Object> formaterTentativePourCorrection(Tentative tentative) {
        Map<String, Object> result = new HashMap<>();

        result.put("id", tentative.getId());
        result.put("etudiant", mapEtudiant(tentative.getEtudiant()));
        result.put("examen", mapExamen(tentative.getExamen()));
        result.put("statut", tentative.getStatut());
        result.put("dateSoumission", tentative.getDateSoumission());
        result.put("noteFinale", tentative.getNoteFinale());
        result.put("estNoteFinaleCalculee", tentative.isEstNoteFinaleCalculee());
        result.put("dateCalculNote", tentative.getDateCalculNote());

        List<Map<String, Object>> questions = new ArrayList<>();

        for (Question question : tentative.getExamen().getQuestions()) {
            Map<String, Object> qMap = new HashMap<>();
            qMap.put("id", question.getId());
            qMap.put("enonce", question.getEnonce());
            qMap.put("bareme", question.getBareme());
            qMap.put("type", question.getClass().getSimpleName());

            ReponseDonnee reponse = tentative.trouverReponse(question.getId());
            if (reponse != null) {
                Map<String, Object> rMap = new HashMap<>();
                rMap.put("id", reponse.getId());
                rMap.put("contenu", reponse.getContenu());
                rMap.put("notePartielle", reponse.getNotePartielle());
                rMap.put("estCorrigee", reponse.isEstCorrigee());
                rMap.put("dateCorrection", reponse.getDateCorrection());
                qMap.put("reponse", rMap);
            }

            questions.add(qMap);
        }

        result.put("questions", questions);

        return result;
    }


    private Map<String, Object> mapEtudiant(Etudiant etudiant) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", etudiant.getId());
        map.put("nom", etudiant.getNom());
        map.put("prenom", etudiant.getPrenom());
        map.put("email", etudiant.getEmail());
        return map;
    }

    private Map<String, Object> mapExamen(Examen examen) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", examen.getId());
        map.put("titre", examen.getTitre());
        map.put("description", examen.getDescription());
        map.put("notesVisibles", examen.isNotesVisibles());
        map.put("datePublicationNotes", examen.getDatePublicationNotes());
        return map;
    }

    private UUID getEnseignantCourantId(Authentication authentication) {
        String email = authentication.getName();
        ca.uqac.examgu.model.Enseignant enseignant = enseignantService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Enseignant non trouvé"));
        return enseignant.getId();
    }
    private UUID getEtudiantCourantId(Authentication authentication) {
        String email = authentication.getName();
        Etudiant etudiant = etudiantService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Etudiant non trouvé"));
        return etudiant.getId();
    }

}