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

    // ===== DTO =====
    public record LoginRequest(String email, String motDePasse) {}
    public record LoginResponse(
            String id,
            String code,
            String role,
            String email,
            String codePermanent
    ) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Utilisateur u = examGuSystem.authentifier(req.email(), req.motDePasse());
        if (u == null) {
            return ResponseEntity.status(401).body("Identifiants invalides.");
        }

        return ResponseEntity.ok(
                new LoginResponse(
                        u.getId().toString(),
                        u.getCode(),
                        u.getRole().name(),
                        u.getEmail(),
                        u.getCodePermanent()
                )
        );
    }
}
