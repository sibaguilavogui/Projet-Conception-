package ca.uqac.examgu.controller;

import ca.uqac.examgu.dto.ExamenDTO;
import ca.uqac.examgu.dto.QuestionAChoixDTO;
import ca.uqac.examgu.dto.QuestionADeveloppementDTO;
import ca.uqac.examgu.model.*;
import ca.uqac.examgu.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/examens")
public class ExamenController {
    private final ExamenService examenService;
    private final QuestionService questionService;
    private final EnseignantService enseignantService;
    private final EtudiantService etudiantService;
    private final TentativeService tentativeService;

    public ExamenController(ExamenService service, QuestionService questionService,
                            EnseignantService enseignantService, EtudiantService etudiantService,
                            TentativeService tentativeService) {
        this.examenService = service;
        this.questionService = questionService;
        this.enseignantService = enseignantService;
        this.etudiantService = etudiantService;
        this.tentativeService = tentativeService;
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
                    return ResponseEntity.badRequest().body("Question Vrai/Faux ou QCM ?");
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

    @DeleteMapping("/{idExamen}/{idQuestion}")
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

    private UUID getEnseignantCourantId(Authentication authentication) {
        String email = authentication.getName();
        ca.uqac.examgu.model.Enseignant enseignant = enseignantService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Enseignant non trouvé"));
        return enseignant.getId();
    }

}

