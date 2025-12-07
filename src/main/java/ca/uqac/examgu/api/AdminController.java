package ca.uqac.examgu.api;

import ca.uqac.examgu.application.ExamGuSystem;
import ca.uqac.examgu.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ExamGuSystem examGuSystem;

    public AdminController(ExamGuSystem examGuSystem) {
        this.examGuSystem = examGuSystem;
    }

    // =====================================================================
    // 1. CRÉATION D’UN UTILISATEUR (ENSEIGNANT / ÉTUDIANT / ADMIN)
    // =====================================================================
    @PostMapping("/utilisateurs")
    public ResponseEntity<?> creerUtilisateur(
            @RequestHeader("X-User-Id") String adminRef,
            @RequestBody CreerUtilisateurDto dto) {

        Utilisateur admin = examGuSystem.trouverUtilisateurParRef(adminRef);
        if (!(admin instanceof Admin a)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Accès refusé : ADMIN requis.");
        }

        Utilisateur u;
        if (dto.role() == Role.ENSEIGNANT) {
            u = new Enseignant(dto.email(), dto.motDePasse(), dto.prenom(), dto.nom(),
                    dto.departement(), dto.dateNaissance());
        } else if (dto.role() == Role.ETUDIANT) {
            u = new Etudiant(dto.email(), dto.motDePasse(), dto.prenom(), dto.nom(),
                    dto.departement(), dto.dateNaissance());
        } else if (dto.role() == Role.ENSEIGNANT) {
            u = new Enseignant(dto.email(), dto.motDePasse(), dto.prenom(), dto.nom(),
                    dto.departement(), dto.dateNaissance());
        } else {
            return ResponseEntity.badRequest()
                    .body("Rôle invalide : seuls ETUDIANT et ENSEIGNANT sont autorisés.");
        }

        try {
            examGuSystem.creerUtilisateur(a, u);
            return ResponseEntity.ok(new UtilisateurDto(
                    u.getId(),
                    u.getCode(),
                    u.getRole(),
                    u.getEmail(),
                    u instanceof Etudiant e ? e.getCodePermanent() : null,
                    u.getPrenom(),
                    u.getNom(),
                    u instanceof Enseignant e ? e.getDepartement() : null,
                    u.getPrenom() + " " + u.getNom()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =====================================================================
    // 2. LISTE DES ÉTUDIANTS
    // =====================================================================
    @GetMapping("/etudiants")
    public List<UtilisateurDto> listerEtudiants(
            @RequestHeader("X-User-Id") String adminRef) {

        verifierAdmin(adminRef);

        return examGuSystem.listerUtilisateurs().stream()
                .filter(u -> u.getRole() == Role.ETUDIANT)
                .map(u -> new UtilisateurDto(
                        u.getId(),
                        u.getCode(),
                        u.getRole(),
                        u.getEmail(),
                        u instanceof Etudiant e ? e.getCodePermanent() : null,
                        u.getPrenom(),
                        u.getNom(),
                        null, // département inutile pour étudiant
                        u.getPrenom() + " " + u.getNom()
                ))
                .toList();
    }

    // =====================================================================
    // 3. LISTE DES ENSEIGNANTS (C’EST CE QUI MANQUAIT !)
    // =====================================================================
    @GetMapping("/enseignants")
    public List<UtilisateurDto> listerEnseignants(
            @RequestHeader("X-User-Id") String adminRef) {

        verifierAdmin(adminRef);

        return examGuSystem.listerUtilisateurs().stream()
                .filter(u -> u.getRole() == Role.ENSEIGNANT)
                .map(u -> new UtilisateurDto(
                        u.getId(),
                        u.getCode(),
                        u.getRole(),
                        u.getEmail(),
                        null, // pas de code permanent pour un enseignant
                        u.getPrenom(),
                        u.getNom(),
                        u instanceof Enseignant e ? e.getDepartement() : "",
                        u.getPrenom() + " " + u.getNom()
                ))
                .toList();
    }

    // =====================================================================
    // 4. LISTE DES EXAMENS
    // =====================================================================
    @GetMapping("/examens")
    public List<ExamenResumeDto> listerExamens(
            @RequestHeader("X-User-Id") String adminRef) {

        verifierAdmin(adminRef);

        Collection<Examen> exams = examGuSystem.listerExamens();
        return exams.stream()
                .map(ex -> new ExamenResumeDto(ex.getId(), ex.getCode(), ex.getEtat(), ex.getTitre()))
                .toList();
    }

    // =====================================================================
    // 5. INSCRIRE UN ÉTUDIANT À UN EXAMEN
    // =====================================================================
    @PostMapping("/examens/{examenRef}/inscriptions")
    public ResponseEntity<?> inscrireEtudiant(
            @RequestHeader("X-User-Id") String adminRef,
            @PathVariable String examenRef,
            @RequestBody InscriptionDto dto) {

        Utilisateur admin = examGuSystem.trouverUtilisateurParRef(adminRef);
        if (!(admin instanceof Admin a)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Accès refusé : ADMIN requis.");
        }

        Examen ex = examGuSystem.trouverExamenParRef(examenRef);
        if (ex == null) {
            return ResponseEntity.badRequest().body("Examen introuvable : " + examenRef);
        }

        Utilisateur u = examGuSystem.trouverUtilisateurParRef(dto.etudiantRef());
        if (!(u instanceof Etudiant etu)) {
            return ResponseEntity.badRequest().body("Étudiant introuvable : " + dto.etudiantRef());
        }

        try {
            Inscription ins = examGuSystem.inscrire(a, ex.getId(), etu.getId());
            return ResponseEntity.ok(new InscriptionResultDto(
                    ex.getId(), ex.getCode(), etu.getId(), etu.getCode(), ins.estActive()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =====================================================================
    // Méthode utilitaire pour éviter de dupliquer la vérification admin
    // =====================================================================
    private void verifierAdmin(String adminRef) {
        Utilisateur admin = examGuSystem.trouverUtilisateurParRef(adminRef);
        if (!(admin instanceof Admin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé : ADMIN requis.");
        }
    }

    // =====================================================================
    // DTOs (records) – VERSION ÉTENDUE POUR LE FRONT
    // =====================================================================
    public record CreerUtilisateurDto(
            String email,
            String motDePasse,
            String prenom,
            String nom,
            String departement,
            LocalDate dateNaissance,
            Role role) {}

    /** DTO renvoyé pour étudiants ET enseignants */
    public record UtilisateurDto(
            UUID id,
            String code,
            Role role,
            String email,
            String codePermanent,   // null pour enseignant
            String prenom,
            String nom,
            String departement,     // null pour étudiant
            String nomComplet       // "Prénom Nom" – très pratique côté front
    ) {
        // Constructeur de compatibilité pour l’ancien code (si jamais utilisé ailleurs)
        public UtilisateurDto(UUID id, String code, Role role, String email, String codePermanent) {
            this(id, code, role, email, codePermanent, null, null, null, null);
        }
    }

    public record ExamenResumeDto(UUID id, String code, EtatExamen etat, String titre) {}

    public record InscriptionDto(String etudiantRef) {}

    public record InscriptionResultDto(
            UUID examenId,
            String examenCode,
            UUID etudiantId,
            String etudiantCode,
            boolean active) {}
}