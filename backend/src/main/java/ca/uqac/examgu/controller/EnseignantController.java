package ca.uqac.examgu.controller;

import ca.uqac.examgu.dto.*;
import ca.uqac.examgu.model.*;
import ca.uqac.examgu.service.EnseignantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/enseignants")
public class EnseignantController {

    private final EnseignantService enseignantService;

    public EnseignantController(EnseignantService enseignantService) {
        this.enseignantService = enseignantService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> registerEnseignant(@RequestBody Enseignant e) {
        return enseignantService.registerEnseignant(e);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Enseignant>> listerEnseignants() {
        return ResponseEntity.ok(enseignantService.listerEnseignants());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerEnseignant(@PathVariable UUID id) {
        enseignantService.supprimerEnseignant(id);
        return ResponseEntity.ok().build();
    }


}