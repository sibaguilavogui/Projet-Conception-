package ca.uqac.examgu.service;

import ca.uqac.examgu.model.Utilisateur;
import ca.uqac.examgu.model.Enumerations.TypeEvenement;
import ca.uqac.examgu.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final JournalisationService journalisationService;
    private final UtilisateurRepository utilisateurRepository;
    private final AuthenticationManager authenticationManager;

    public AuthService(UtilisateurRepository utilisateurRepository,
                       JournalisationService journalisationService,
                       AuthenticationManager authenticationManager) {
        this.utilisateurRepository = utilisateurRepository;
        this.journalisationService = journalisationService;
        this.authenticationManager = authenticationManager;
    }

    public static class LoginResponse {
        private String message;
        private UUID userId;
        private String email;
        private String nom;
        private String prenom;
        private String role;
        private LocalDateTime dateConnexion;

        public LoginResponse(String message, UUID userId, String email, String nom,
                             String prenom, String role, LocalDateTime dateConnexion) {
            this.message = message;
            this.userId = userId;
            this.email = email;
            this.nom = nom;
            this.prenom = prenom;
            this.role = role;
            this.dateConnexion = dateConnexion;
        }

        // Getters et Setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }

        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public LocalDateTime getDateConnexion() { return dateConnexion; }
        public void setDateConnexion(LocalDateTime dateConnexion) { this.dateConnexion = dateConnexion; }
    }

    public ResponseEntity<?> login(String email, String motDePasse) {
        try {
            // Validation des paramètres
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("L'email est obligatoire");
            }
            if (motDePasse == null || motDePasse.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le mot de passe est obligatoire");
            }

            // Vérifier si l'utilisateur existe
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByEmail(email);
            if (utilisateurOpt.isEmpty()) {
                journalisationService.log(
                        TypeEvenement.LOGIN,
                        email,
                        "Tentative de connexion échouée - Utilisateur non trouvé"
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Identifiants invalides");
            }

            // Authentifier avec Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, motDePasse)
            );
            System.out.println(authentication.getName());

            // Mettre à jour le contexte de sécurité
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Récupérer l'utilisateur authentifié
            Utilisateur utilisateur = utilisateurOpt.get();

            journalisationService.log(
                    TypeEvenement.LOGIN,
                    utilisateur.getEmail(),
                    "Connexion réussie - Rôle: " + utilisateur.getRole()
            );

            // Préparer la réponse
            LoginResponse response = new LoginResponse(
                    "Connexion réussie",
                    utilisateur.getId(),
                    utilisateur.getEmail(),
                    utilisateur.getNom(),
                    utilisateur.getPrenom(),
                    utilisateur.getRole().name(),
                    LocalDateTime.now()
            );

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            journalisationService.log(
                    TypeEvenement.LOGIN,
                    email,
                    "Tentative de connexion échouée - Mot de passe incorrect"
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Identifiants invalides");

        } catch (Exception e) {
            journalisationService.log(
                    TypeEvenement.LOGIN,
                    email,
                    "Erreur lors de la connexion: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'authentification: " + e.getMessage());
        }
    }

}