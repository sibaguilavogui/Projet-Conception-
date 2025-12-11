package ca.uqac.examgu.controller;

import ca.uqac.examgu.dto.LoginRequest;
import ca.uqac.examgu.model.Enumerations.TypeEvenement;
import ca.uqac.examgu.security.JwtService;
import ca.uqac.examgu.service.AuthService;
import ca.uqac.examgu.service.JournalisationService;
import ca.uqac.examgu.service.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JournalisationService journalisationService;

    public AuthController(AuthService service, JwtService jwtService, TokenBlacklistService tokenBlacklistService, JournalisationService journalisationService) {
        this.service = service;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.journalisationService = journalisationService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        ResponseEntity<?> loginResponse = service.login(req.getEmail(), req.getPassword());

        if (loginResponse.getStatusCode() == HttpStatus.OK) {
            AuthService.LoginResponse userInfo = (AuthService.LoginResponse) loginResponse.getBody();
            Map<String, String> token = jwtService.generate(req.getEmail());

            if (token != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("user", userInfo);
                response.put("token", token.get("bearer"));
                return ResponseEntity.ok(response);
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Échec de l'authentification");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                String email = jwtService.extractEmail(token);

                tokenBlacklistService.blacklistToken(token);
                SecurityContextHolder.clearContext();

                journalisationService.log(
                        TypeEvenement.LOGOUT,
                        "ADMIN",
                        "Déconnexion réussie"
                );
                return ResponseEntity.ok("Déconnexion réussie");
            }

            return ResponseEntity.badRequest().body("Token manquant");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }

}
