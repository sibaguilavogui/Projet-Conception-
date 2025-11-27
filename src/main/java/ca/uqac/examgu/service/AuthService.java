package ca.uqac.examgu.service;

import ca.uqac.examgu.model.Enseignant;
import ca.uqac.examgu.model.Etudiant;
import ca.uqac.examgu.model.Utilisateur;
import ca.uqac.examgu.repository.UtilisateurRepository;
import ca.uqac.examgu.security.UtilisateurDetailsImpl;
import ca.uqac.examgu.model.TypeEvenement;
import ca.uqac.examgu.service.JournalisationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JournalisationService journalisationService;
    private final UtilisateurRepository repo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final UtilisateurDetailsServiceImpl utilisateurDetailsService;

    public AuthService(UtilisateurRepository repo,
                       PasswordEncoder encoder,
                       JournalisationService journalisationService, AuthenticationManager authenticationManager, UtilisateurDetailsServiceImpl utilisateurDetailsService) {
        this.repo = repo;
        this.encoder = encoder;
        this.journalisationService = journalisationService;
        this.authenticationManager = authenticationManager;
        this.utilisateurDetailsService = utilisateurDetailsService;
    }


    public ResponseEntity<?> registerEtudiant(Etudiant e) {
        e.setMotDePasseHash(encoder.encode(e.getMotDePasseHash()));
        Utilisateur saved = repo.save(e);
        journalisationService.log(
                TypeEvenement.LOGIN,
                saved.getEmail(),
                "Compte créé avec le rôle " + saved.getRole()
        );
        return ResponseEntity.ok().body("Compte etudiant créé");
    }

    public ResponseEntity<?> registerEnseignant(Enseignant e) {
        e.setMotDePasseHash(encoder.encode(e.getMotDePasseHash()));
        Utilisateur saved = repo.save(e);
        journalisationService.log(
                TypeEvenement.LOGIN,
                saved.getEmail(),
                "Compte créé avec le rôle " + saved.getRole()
        );
        return ResponseEntity.ok().body("Compte enseignant créé");
    }

    public ResponseEntity<?> login(String email, String motDePasse) {
        try {
            System.out.println("ICII0");
            // 1. Charger l'utilisateur via ton service Spring Security (qui lui lit la base)
            var userDetails = utilisateurDetailsService.loadUserByUsername(email);

            // 2. Vérifier le mot de passe avec BCrypt
            if (!encoder.matches(motDePasse, userDetails.getPassword())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("LOGIN FAIL ❌ accès refusé");
            }

            // 3. Créer un token Spring avec les authorities réelles
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            // 4. Injecter dans le contexte de sécurité
            SecurityContextHolder.getContext().setAuthentication(authToken);

            return ResponseEntity.ok("LOGIN OK ✅");

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("LOGIN FAIL ❌ accès refusé");
        }
    }
}




