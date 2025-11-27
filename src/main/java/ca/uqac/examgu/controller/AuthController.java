package ca.uqac.examgu.controller;

import ca.uqac.examgu.dto.LoginRequest;
import ca.uqac.examgu.model.Enseignant;
import ca.uqac.examgu.model.Etudiant;
import ca.uqac.examgu.model.Utilisateur;
import ca.uqac.examgu.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/registerEtudiant")
    public ResponseEntity<?> registerEtudiant(@RequestBody Etudiant e) {
        return service.registerEtudiant(e);
    }

    @PostMapping("/registerEnseignant")
    public ResponseEntity<?> registerEnseignant(@RequestBody Enseignant e) {
        return service.registerEnseignant(e);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        return service.login(req.getEmail(), req.getPassword());
    }

}
