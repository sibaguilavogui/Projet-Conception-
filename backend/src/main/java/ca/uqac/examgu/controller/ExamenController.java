package ca.uqac.examgu.controller;

import ca.uqac.examgu.dto.ExamenDTO;
import ca.uqac.examgu.dto.QuestionAChoixDTO;
import ca.uqac.examgu.dto.QuestionADeveloppementDTO;
import ca.uqac.examgu.model.*;
import ca.uqac.examgu.repository.TentativeRepository;
import ca.uqac.examgu.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/examens")
public class ExamenController {
    private final ExamenService examenService;
    private final QuestionService questionService;
    private final EnseignantService enseignantService;
    private final EtudiantService etudiantService;
    private final TentativeService tentativeService;
    private final InscriptionService inscriptionService;
    private final TentativeRepository tentativeRepository;

    public ExamenController(ExamenService service, QuestionService questionService,
                            EnseignantService enseignantService, EtudiantService etudiantService,
                            TentativeService tentativeService, InscriptionService inscriptionService, TentativeRepository tentativeRepository) {
        this.examenService = service;
        this.questionService = questionService;
        this.enseignantService = enseignantService;
        this.etudiantService = etudiantService;
        this.tentativeService = tentativeService;
        this.inscriptionService = inscriptionService;
        this.tentativeRepository = tentativeRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> creerExamen(@RequestBody ExamenDTO examen,
                                         Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Enseignant> enseignantOpt = enseignantService.findByEmail(email);

            if (enseignantOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Utilisateur non trouvé ou non autorisé");
            }

            Enseignant enseignant = enseignantOpt.get();

            if (examen.getTitre() == null || examen.getTitre().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le titre est obligatoire");
            }

            Examen examenCree = examenService.creer(examen, enseignant);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Examen créé avec succès");
            response.put("examen", examenCree);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la création de l'examen: " + e.getMessage());
        }
    }

    @PostMapping("/{idExamen}/question-choix")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> ajouterQuestionChoix(@PathVariable UUID idExamen,
                                                  @RequestBody QuestionAChoixDTO request,
                                                  Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Enseignant> enseignantOpt = enseignantService.findByEmail(email);

            if (enseignantOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Utilisateur non trouvé");
            }

            Enseignant enseignant = enseignantOpt.get();
            Optional<Examen> optionalExamen = examenService.trouverParId(idExamen);
            if (optionalExamen.isPresent()){
                if(!optionalExamen.get().getCreateur().equals(enseignant)){
                    throw new SecurityException("Seul le créateur de l'examen peut ajouter des questions");
                }

                if (request.getEnonce() == null || request.getEnonce().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("L'énoncé est obligatoire");
                }
                if (request.getTypeChoix()==null) {
                    return ResponseEntity.badRequest().body("Reponse unique ou multiple ?");
                }
                if (request.getPolitiqueCorrectionQCM()==null) {
                    return ResponseEntity.badRequest().body("Choisissez une politique de correction");
                }

                return ResponseEntity.ok().body(
                        questionService.ajouterQuestionChoix(request, optionalExamen.get())
                );

            }
            return ResponseEntity.badRequest().body("Examen introuvable");

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'ajout de la question: " + e.getMessage());
        }
    }

    @PostMapping("/{idExamen}/question-developpement")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> ajouterQuestionDeveloppement(@PathVariable UUID idExamen,
                                                          @RequestBody QuestionADeveloppementDTO request,
                                                          Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Enseignant> enseignantOpt = enseignantService.findByEmail(email);

            if (enseignantOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Utilisateur non trouvé");
            }

            Enseignant enseignant = enseignantOpt.get();
            Optional<Examen> optionalExamen = examenService.trouverParId(idExamen);
            if (optionalExamen.isPresent()){
                if(!optionalExamen.get().getCreateur().equals(enseignant)){
                    throw new SecurityException("Seul le créateur de l'examen peut ajouter des questions");
                }

                if (request.getEnonce() == null || request.getEnonce().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("L'énoncé est obligatoire");
                }

                return ResponseEntity.ok().body(
                        questionService.ajouterQuestionDeveloppement(request, optionalExamen.get())
                );

            }
            return ResponseEntity.badRequest().body("Examen introuvable");

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'ajout de la question: " + e.getMessage());
        }
    }

    @DeleteMapping("/{idExamen}/questions/{idQuestion}")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> supprimerQuestion(@PathVariable UUID idExamen,
                                               @PathVariable UUID idQuestion,
                                               Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Enseignant> enseignantOpt = enseignantService.findByEmail(email);

            if (enseignantOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Utilisateur non trouvé");
            }

            Enseignant enseignant = enseignantOpt.get();
            Optional<Examen> optionalExamen = examenService.trouverParId(idExamen);
            if (optionalExamen.isPresent()){
                if(!optionalExamen.get().getCreateur().equals(enseignant)){
                    throw new SecurityException("Seul le créateur de l'examen peut supprimer des questions");
                }

                boolean supprime = questionService.supprimerQuestion(idQuestion, optionalExamen.get());
                if (!supprime) {
                    return ResponseEntity.notFound().build();
                }
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Question supprimée avec succès");
                return ResponseEntity.ok(response);

            }
            return ResponseEntity.badRequest().body("Examen introuvable");

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la suppression de la question: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getExamens(Authentication auth) {
        boolean estEnseignant = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ENSEIGNANT"));

        boolean estEtudiant = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ETUDIANT"));

        boolean estAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        try {
            if (estEnseignant) {
                Enseignant e = enseignantService.findByEmail(auth.getName())
                        .orElseThrow(() -> new RuntimeException("Enseignant non trouvée"));
                return ResponseEntity.ok(examenService.listerExamenEnseignant(e));
            }
            if (estEtudiant) {
                Etudiant e = etudiantService.findByEmail(auth.getName())
                        .orElseThrow(() -> new RuntimeException("Etudiant non trouvée"));
                return ResponseEntity.ok(examenService.listerExamenEtudiant(e));
            }
            if (estAdmin) {
                return ResponseEntity.ok(examenService.lister());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération des examens: " + e.getMessage());
        }

        return ResponseEntity.status(403).body("Accès refusé");
    }


    @PutMapping("/{examenId}")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> modifierExamen(@PathVariable UUID examenId,
                                            @RequestBody ExamenDTO request,
                                            Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Enseignant> enseignantOpt = enseignantService.findByEmail(email);

            if (enseignantOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Utilisateur non trouvé ou non autorisé");
            }

            Enseignant enseignant = enseignantOpt.get();

            Examen examenModifie = examenService.modifierExamen(examenId, request, enseignant);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Examen modifié avec succès");
            response.put("examen", examenModifie);

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la modification de l'examen: " + e.getMessage());
        }
    }

    @PutMapping("/{examenId}/planifier")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> planifierExamen(@PathVariable UUID examenId,
                                             @RequestBody ExamenDTO request,
                                             Authentication authentication){
        try {
            String email = authentication.getName();
            Optional<Enseignant> enseignantOpt = enseignantService.findByEmail(email);

            if (enseignantOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Utilisateur non trouvé ou non autorisé");
            }

            Enseignant enseignant = enseignantOpt.get();

            Examen examenPlanifie = examenService.planifierExamen(examenId, request, enseignant);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Examen planifié avec succès");
            response.put("examen", examenPlanifie);

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la planification de l'examen: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getExamen(@PathVariable UUID id, Authentication auth) {
        boolean estEnseignant = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ENSEIGNANT"));

        boolean estEtudiant = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ETUDIANT"));

        boolean estAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        try {
            if (estEnseignant) {
                Enseignant e = enseignantService.findByEmail(auth.getName())
                        .orElseThrow(() -> new RuntimeException("Enseignant non trouvée"));
                return ResponseEntity.ok(examenService.listerExamenEnseignant(e)
                        .stream()
                        .filter(examen -> examen.getId().equals(id))
                        .findFirst()
                        .orElse(null));
            }
            if (estEtudiant) {
                Etudiant e = etudiantService.findByEmail(auth.getName())
                        .orElseThrow(() -> new RuntimeException("Etudiant non trouvée"));
                return ResponseEntity.ok(examenService.listerExamenEtudiant(e)
                        .stream()
                        .filter(examen -> examen.getId().equals(id))
                        .findFirst()
                        .orElse(null));
            }
            if (estAdmin) {
                return ResponseEntity.ok(examenService.lister()
                        .stream()
                        .filter(examen -> examen.getId().equals(id))
                        .findFirst()
                        .orElse(null));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération des examens: " + e.getMessage());
        }

        return ResponseEntity.status(403).body("Accès refusé");
    }


    @PutMapping("/{examenId}/marquer-pret")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> marquerExamenPret(@PathVariable UUID examenId,
                                               Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Enseignant> enseignantOpt = enseignantService.findByEmail(email);

            if (enseignantOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Utilisateur non trouvé");
            }

            Enseignant enseignant = enseignantOpt.get();
            Optional<Examen> optionalExamen = examenService.trouverParId(examenId);

            if (optionalExamen.isEmpty()) {
                return ResponseEntity.badRequest().body("Examen introuvable");
            }

            Examen examen = optionalExamen.get();
            return examenService.marquerExamenPret(examen, enseignant);

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }


    @GetMapping("/{examenId}/tentatives")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> getTentativesExamen(@PathVariable UUID examenId, Authentication authentication) {
        try {
            UUID enseignantId = getEnseignantCourantId(authentication);

            List<Tentative> tentatives = tentativeService.getTentativesExamen(examenId, enseignantId);

            return ResponseEntity.ok(tentatives);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération: " + e.getMessage());
        }
    }

    @GetMapping("/{examenId}/inscriptions")
    @PreAuthorize("hasRole('ETUDIANT') or hasRole('ADMIN')")
    public ResponseEntity<?> getInscriptionsExamen(@PathVariable UUID examenId, Authentication auth) {
        boolean estEnseignant = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ENSEIGNANT"));

        boolean estAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        try {
            if (estEnseignant) {
                Enseignant e = enseignantService.findByEmail(auth.getName())
                        .orElseThrow(() -> new RuntimeException("Enseignant non trouvée"));
                return ResponseEntity.ok(examenService.listerInscriptionsEnseignant(examenId, e));
            }
            if (estAdmin) {
                return ResponseEntity.ok(inscriptionService.getInscriptions().stream()
                        .filter(inscription -> inscription.getExamen().getId().equals(examenId))
                        .collect(Collectors.toList())
                );
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération des examens: " + e.getMessage());
        }

        return ResponseEntity.status(403).body("Accès refusé");
    }


    @DeleteMapping("/{examenId}")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> supprimerExamen(@PathVariable UUID examenId, Authentication authentication) {
        try {
            String email = authentication.getName();
            Enseignant enseignant = enseignantService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Enseignant non trouvé"));

            Examen examen = examenService.trouverParId(examenId)
                    .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

            // Vérifier que l'enseignant est le créateur
            if (!examen.getCreateur().getId().equals(enseignant.getId())) {
                return ResponseEntity.status(403).body("Vous n'êtes pas le créateur de cet examen");
            }

            // Supprimer l'examen en utilisant le service
            examenService.supprimerExamen(examenId, enseignant);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Examen supprimé avec succès");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la suppression: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{examenId}/{etudiantId}")
    public ResponseEntity<?> inscrireEtudiant(@PathVariable UUID examenId, @PathVariable UUID etudiantId) {
        try {
            Inscription inscription = inscriptionService.inscrireEtudiant(examenId, etudiantId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", inscription.getId());
            response.put("statut", inscription.getStatut());
            response.put("message", "Étudiant inscrit avec succès");
            response.put("etudiantEmail", inscription.getEtudiant().getEmail());
            response.put("examenTitre", inscription.getExamen().getTitre());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{examenId}/{etudiantId}")
    public ResponseEntity<?> desinscrireEtudiant(@PathVariable UUID examenId, @PathVariable UUID etudiantId) {
        inscriptionService.desinscrireEtudiant(examenId, etudiantId);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/{examenId}/demarrer-tentative")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> demarrerTentative(@PathVariable UUID examenId, Authentication authentication) {
        try {
            UUID etudiantId = getEtudiantCourantId(authentication);
            Tentative tentative = tentativeService.demarrerTentative(examenId, etudiantId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Tentative démarrée/récupérée avec succès");
            response.put("tentative", tentative);
            response.put("tempsRestant", tentative.tempsRestant());
            response.put("estNouvelleTentative", tentative.getDateCreation().isAfter(LocalDateTime.now().minusMinutes(1)));

            // Inclure les réponses existantes si c'est une tentative récupérée
            if (tentative.getReponses() != null && !tentative.getReponses().isEmpty()) {
                List<Map<String, Object>> reponsesExistantes = tentative.getReponses().stream()
                        .map(r -> {
                            Map<String, Object> reponseMap = new HashMap<>();
                            reponseMap.put("questionId", r.getQuestion().getId());
                            reponseMap.put("contenu", r.getContenu());
                            reponseMap.put("dateMaj", r.getDateMaj());
                            return reponseMap;
                        })
                        .collect(Collectors.toList());
                response.put("reponsesExistantes", reponsesExistantes);
            }

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors du démarrage: " + e.getMessage());
        }
    }


    @GetMapping("/{examenId}/verifier-tentative")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> verifierTentativeExistante(@PathVariable UUID examenId, Authentication authentication) {
        try {
            UUID etudiantId = getEtudiantCourantId(authentication);

            Optional<Tentative> tentativeExistante = tentativeRepository
                    .findByExamenIdAndEtudiantId(examenId, etudiantId);

            Map<String, Object> response = new HashMap<>();

            if (tentativeExistante.isPresent()) {
                Tentative tentative = tentativeExistante.get();
                response.put("tentativeExistante", true);
                response.put("statut", tentative.getStatut());
                response.put("tentativeId", tentative.getId());
                response.put("tempsRestant", tentative.tempsRestant());
                response.put("estExpiree", tentative.estExpiree());
                response.put("dateCreation", tentative.getDateCreation());
                response.put("dateSoumission", tentative.getDateSoumission());
            } else {
                response.put("tentativeExistante", false);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la vérification: " + e.getMessage());
        }
    }


    @PostMapping("/{examenId}/calculer-notes-finales")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> calculerNotesFinales(@PathVariable UUID examenId, Authentication auth) {
        try {
            UUID enseignantId = getEnseignantCourantId(auth);
            Examen examen = examenService.trouverParId(examenId)
                    .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

            // Vérifier que l'enseignant est le créateur
            if (!examen.getCreateur().getId().equals(enseignantId)) {
                return ResponseEntity.status(403).body("Accès refusé");
            }

            examenService.calculerNotesFinalesExamen(examenId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Notes finales calculées avec succès");
            response.put("examenId", examenId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }


    @PostMapping("/{examenId}/publier-notes")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> publierNotes(@PathVariable UUID examenId, Authentication auth) {
        try {
            UUID enseignantId = getEnseignantCourantId(auth);
            Examen examen = examenService.trouverParId(examenId)
                    .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

            // Vérifier que l'enseignant est le créateur
            if (!examen.getCreateur().getId().equals(enseignantId)) {
                return ResponseEntity.status(403).body("Accès refusé");
            }

            // Vérifier que l'examen est terminé
            if (examen.getDateFin() != null && examen.getDateFin().isAfter(LocalDateTime.now())) {
                return ResponseEntity.badRequest()
                        .body("La publication des notes n'est disponible qu'après la date de fin de l'examen");
            }

            Examen examenMisAJour = examenService.publierNotes(examenId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Notes publiées avec succès");
            response.put("notesVisibles", examenMisAJour.isNotesVisibles());
            response.put("datePublication", examenMisAJour.getDatePublicationNotes());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }


    @PostMapping("/{examenId}/masquer-notes")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> masquerNotes(@PathVariable UUID examenId, Authentication auth) {
        try {
            UUID enseignantId = getEnseignantCourantId(auth);
            Examen examen = examenService.trouverParId(examenId)
                    .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

            // Vérifier que l'enseignant est le créateur
            if (!examen.getCreateur().getId().equals(enseignantId)) {
                return ResponseEntity.status(403).body("Accès refusé");
            }

            Examen examenMisAJour = examenService.masquerNotes(examenId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Notes masquées avec succès");
            response.put("notesVisibles", examenMisAJour.isNotesVisibles());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    @GetMapping("/{examenId}/ma-note")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> getMaNote(@PathVariable UUID examenId, Authentication auth) {
        try {
            UUID etudiantId = getEtudiantCourantId(auth);

            Double note = examenService.getNoteEtudiant(examenId, etudiantId);
            if(note<0){
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Les notes ne sont pas encore disponibles ou non publiées");
            }
            Map<String, Object> response = new HashMap<>();
            response.put("note", note);
            response.put("examenId", examenId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }


    @PostMapping("/{examenId}/corriger-automatiquement")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> corrigerAutomatiquement(@PathVariable UUID examenId, Authentication auth) {
        try {
            UUID enseignantId = getEnseignantCourantId(auth);
            Examen examen = examenService.trouverParId(examenId)
                    .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

            // Vérifier que l'enseignant est le créateur
            if (!examen.getCreateur().getId().equals(enseignantId)) {
                return ResponseEntity.status(403).body("Accès refusé");
            }

            // Vérifier que l'examen est terminé
            if (examen.getDateFin() != null && examen.getDateFin().isAfter(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body("La correction automatique n'est disponible qu'après la date de fin de l'examen");
            }

            // Corriger automatiquement toutes les tentatives
            Map<String, Object> resultat = examenService.corrigerAutomatiquement(examenId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Correction automatique terminée");
            response.put("resultat", resultat);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }


    @GetMapping("/{examenId}/est-termine")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> estExamenTermine(@PathVariable UUID examenId, Authentication auth) {
        try {
            UUID enseignantId = getEnseignantCourantId(auth);
            Examen examen = examenService.trouverParId(examenId)
                    .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

            if (!examen.getCreateur().getId().equals(enseignantId)) {
                return ResponseEntity.status(403).body("Accès refusé");
            }

            boolean termine = examen.getDateFin() != null &&
                    examen.getDateFin().isBefore(LocalDateTime.now());

            Map<String, Object> response = new HashMap<>();
            response.put("examenId", examenId);
            response.put("titre", examen.getTitre());
            response.put("termine", termine);
            response.put("dateFin", examen.getDateFin());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }



    @GetMapping("/{examenId}/questions")
    public ResponseEntity<?> getQuestionsExamen(@PathVariable UUID examenId, Authentication authentication) {
        try {
            String email = authentication.getName();
            boolean estEnseignant = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ENSEIGNANT"));
            boolean estEtudiant = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ETUDIANT"));
            boolean estAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            Examen examen = examenService.trouverParId(examenId)
                    .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

            if (estEnseignant) {
                Enseignant enseignant = enseignantService.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Enseignant non trouvé"));

                if (!examen.getCreateur().getId().equals(enseignant.getId()) && !estAdmin) {
                    return ResponseEntity.status(403).body("Seul le créateur de l'examen peut voir les questions");
                }

                // Retourner les questions avec toutes les informations (y compris les réponses correctes)
                List<Map<String, Object>> questions = convertirQuestionsPourEnseignant(examen);
                return ResponseEntity.ok(questions);

            } else if (estEtudiant) {
                Etudiant etudiant = etudiantService.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));

                // Vérifier si l'étudiant est inscrit à l'examen
                if (!examen.estInscrit(etudiant)) {
                    return ResponseEntity.status(403).body("Vous n'êtes pas inscrit à cet examen");
                }

                // Vérifier si l'examen est disponible pour l'étudiant
                if (!examen.estDisponible()) {
                    return ResponseEntity.status(403).body("L'examen n'est pas disponible actuellement");
                }

                // Retourner les questions sans les réponses correctes pour les QCM
                List<Map<String, Object>> questions = convertirQuestionsPourEtudiant(examen);
                return ResponseEntity.ok(questions);

            } else if (estAdmin) {
                // Admin peut voir toutes les informations
                List<Map<String, Object>> questions = convertirQuestionsPourEnseignant(examen);
                return ResponseEntity.ok(questions);
            }

            return ResponseEntity.status(403).body("Accès refusé");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur serveur: " + e.getMessage());
        }
    }



    @GetMapping("/{examenId}/tentatives-non-corrigees")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> getTentativesNonCorrigees(@PathVariable UUID examenId, Authentication auth) {
        try {
            UUID enseignantId = getEnseignantCourantId(auth);
            Examen examen = examenService.trouverParId(examenId)
                    .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

            // Vérifier que l'enseignant est le créateur
            if (!examen.getCreateur().getId().equals(enseignantId)) {
                return ResponseEntity.status(403).body("Accès refusé");
            }

            // Récupérer toutes les tentatives de l'examen
            List<Tentative> tentatives = tentativeService.getTentativesExamen(examenId, enseignantId);

            // Filtrer les tentatives non corrigées
            List<UUID> tentativesNonCorrigees = tentatives.stream()
                    .filter(t -> !t.isEstCorrigee())
                    .map(Tentative::getId)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("examenId", examenId);
            response.put("titreExamen", examen.getTitre());
            response.put("tentativesNonCorrigees", tentativesNonCorrigees);
            response.put("nombreTentativesNonCorrigees", tentativesNonCorrigees.size());
            response.put("nombreTentativesTotal", tentatives.size());

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }


    @GetMapping("/{examenId}/est-totalement-corrige")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> estExamenTotalementCorrige(@PathVariable UUID examenId, Authentication auth) {
        try {
            UUID enseignantId = getEnseignantCourantId(auth);
            Examen examen = examenService.trouverParId(examenId)
                    .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

            // Vérifier que l'enseignant est le créateur
            if (!examen.getCreateur().getId().equals(enseignantId)) {
                return ResponseEntity.status(403).body("Accès refusé");
            }

            // Récupérer toutes les tentatives de l'examen
            List<Tentative> tentatives = tentativeService.getTentativesExamen(examenId, enseignantId);

            // Vérifier si toutes les tentatives sont corrigées
            boolean toutesCorrigees = true;
            List<UUID> tentativesNonCorrigees = new ArrayList<>();

            for (Tentative t : tentatives) {
                if (!t.isEstCorrigee()) {
                    toutesCorrigees = false;
                    tentativesNonCorrigees.add(t.getId());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("examenId", examenId);
            response.put("titreExamen", examen.getTitre());
            response.put("estTotalementCorrige", toutesCorrigees);
            response.put("tentativesNonCorrigees", tentativesNonCorrigees);
            response.put("nombreTentativesNonCorrigees", tentativesNonCorrigees.size());
            response.put("nombreTentativesTotal", tentatives.size());

            if (!toutesCorrigees) {
                response.put("message",
                        String.format("%d tentative(s) sur %d ne sont pas encore corrigées",
                                tentativesNonCorrigees.size(), tentatives.size()));
            } else {
                response.put("message", "Toutes les tentatives sont corrigées");
            }

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }


    @GetMapping("/{examenId}/verifier-correction-disponible")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<?> verifierCorrectionDisponible(@PathVariable UUID examenId, Authentication auth) {
        try {
            UUID enseignantId = getEnseignantCourantId(auth);
            Examen examen = examenService.trouverParId(examenId)
                    .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

            if (!examen.getCreateur().getId().equals(enseignantId)) {
                return ResponseEntity.status(403).body("Accès refusé");
            }

            boolean correctionDisponible = examen.getDateFin() != null &&
                    examen.getDateFin().isBefore(LocalDateTime.now());

            Map<String, Object> response = new HashMap<>();
            response.put("correctionDisponible", correctionDisponible);
            response.put("dateFin", examen.getDateFin());
            response.put("dateActuelle", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> convertirQuestionsPourEnseignant(Examen examen) {
        return examen.getQuestions().stream()
                .map(question -> {
                    Map<String, Object> questionMap = new HashMap<>();
                    questionMap.put("id", question.getId());
                    questionMap.put("enonce", question.getEnonce());
                    questionMap.put("bareme", question.getBareme());
                    questionMap.put("ordre", examen.getQuestions().indexOf(question) + 1);

                    if (question instanceof QuestionAChoix) {
                        QuestionAChoix qChoix = (QuestionAChoix) question;
                        questionMap.put("type", "CHOIX");
                        questionMap.put("typeChoix", qChoix.getTypeChoix());
                        questionMap.put("politiqueCorrectionQCM", qChoix.getPolitiqueCorrectionQCM());

                        // Ajouter les réponses possibles avec l'information de correction
                        List<Map<String, Object>> options = qChoix.getReponsesPossibles().stream()
                                .map(rp -> {
                                    Map<String, Object> optionMap = new HashMap<>();
                                    optionMap.put("id", rp.getId());
                                    optionMap.put("libelle", rp.getLibelle());
                                    optionMap.put("correcte", rp.isCorrecte());
                                    return optionMap;
                                })
                                .collect(Collectors.toList());
                        questionMap.put("options", options);

                    } else if (question instanceof QuestionADeveloppement) {
                        questionMap.put("type", "DEVELOPPEMENT");
                    }

                    return questionMap;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> convertirQuestionsPourEtudiant(Examen examen) {
        return examen.getQuestions().stream()
                .map(question -> {
                    Map<String, Object> questionMap = new HashMap<>();
                    questionMap.put("id", question.getId());
                    questionMap.put("enonce", question.getEnonce());
                    questionMap.put("bareme", question.getBareme());
                    questionMap.put("ordre", examen.getQuestions().indexOf(question) + 1);

                    if (question instanceof QuestionAChoix) {
                        QuestionAChoix qChoix = (QuestionAChoix) question;
                        questionMap.put("type", "CHOIX");
                        questionMap.put("typeChoix", qChoix.getTypeChoix());
                        questionMap.put("politiqueCorrectionQCM", qChoix.getPolitiqueCorrectionQCM());

                        // Pour les étudiants, ne pas inclure l'information de correction
                        List<Map<String, Object>> options = qChoix.getReponsesPossibles().stream()
                                .map(rp -> {
                                    Map<String, Object> optionMap = new HashMap<>();
                                    optionMap.put("id", rp.getId());
                                    optionMap.put("libelle", rp.getLibelle());
                                    // Note: Ne pas inclure "correcte" pour les étudiants
                                    return optionMap;
                                })
                                .collect(Collectors.toList());
                        questionMap.put("options", options);

                    } else if (question instanceof QuestionADeveloppement) {
                        questionMap.put("type", "DEVELOPPEMENT");
                    }

                    return questionMap;
                })
                .collect(Collectors.toList());
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