package ca.uqac.examgu.controller;

import ca.uqac.examgu.model.*;
import ca.uqac.examgu.service.ExamenService;
import ca.uqac.examgu.service.InscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inscriptions")
public class InscriptionController {
    private final InscriptionService inscriptionService;
    private final ExamenService examenService;

    public InscriptionController(InscriptionService inscriptionService, ExamenService examenService) {
        this.inscriptionService = inscriptionService;
        this.examenService = examenService;
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

}

