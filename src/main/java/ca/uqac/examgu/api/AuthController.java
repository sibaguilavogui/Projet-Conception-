package ca.uqac.examgu.api;

import ca.uqac.examgu.application.ExamGuSystem;
import ca.uqac.examgu.domain.Utilisateur;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ExamGuSystem examGuSystem;

    public AuthController(ExamGuSystem examGuSystem) {
        this.examGuSystem = examGuSystem;
    }

    record LoginRequest(String email, String password) {}
    record LoginResponse(String userId, String role, String code) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Utilisateur u = examGuSystem.authentifier(req.email, req.password);
        if (u == null) {
            return ResponseEntity.status(401).body("Identifiants incorrects");
        }
        return ResponseEntity.ok(new LoginResponse(
                u.getCode(),           // ETU-0001, ENS-0001, ADM-0001
                u.getRole().name(),    // ETUDIANT / ENSEIGNANT / ADMIN
                u.getCode()
        ));
    }
}