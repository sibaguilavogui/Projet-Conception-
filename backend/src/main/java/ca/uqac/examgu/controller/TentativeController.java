package ca.uqac.examgu.controller;

import ca.uqac.examgu.model.Etudiant;
import ca.uqac.examgu.model.Tentative;
import ca.uqac.examgu.repository.EnseignantRepository;
import ca.uqac.examgu.repository.EtudiantRepository;
import ca.uqac.examgu.repository.UtilisateurRepository;
import ca.uqac.examgu.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/{examenId}/demarrer")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> demarrerTentative(@PathVariable UUID examenId, Authentication authentication) {
        try {
            UUID etudiantId = getEtudiantCourantId(authentication);

            Tentative tentative = tentativeService.demarrerTentative(examenId, etudiantId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Tentative démarrée avec succès");
            response.put("tentative", tentative);
            response.put("tempsRestant", tentative.tempsRestant());

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors du démarrage: " + e.getMessage());
        }
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