package ca.uqac.examgu.controller;

import ca.uqac.examgu.dto.EtudiantDTO;
import ca.uqac.examgu.model.Etudiant;
import ca.uqac.examgu.repository.EtudiantRepository;
import ca.uqac.examgu.service.EtudiantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/etudiants")
public class EtudiantController {

    private final EtudiantService etudiantService;

    public EtudiantController(EtudiantService etudiantService) {
        this.etudiantService = etudiantService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("{id}")
    public ResponseEntity<Etudiant> modifierEtudiant(@PathVariable UUID id, @RequestBody EtudiantDTO etudiantDTO) {
        return ResponseEntity.ok(etudiantService.modifierEtudiant(id, etudiantDTO));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Etudiant>> listerEtudiants() {
        return ResponseEntity.ok(etudiantService.listerEtudiants());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerEtudiant(@PathVariable UUID id) {
        etudiantService.supprimerEtudiant(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> registerEtudiant(@RequestBody Etudiant e) {
        return etudiantService.registerEtudiant(e);
    }


}