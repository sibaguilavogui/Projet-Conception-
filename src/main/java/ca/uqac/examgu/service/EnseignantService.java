package ca.uqac.examgu.service;

import ca.uqac.examgu.model.*;
import ca.uqac.examgu.model.Enumerations.TypeEvenement;
import ca.uqac.examgu.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EnseignantService {

    private final JournalisationService journalisationService;
    private final UtilisateurRepository utilisateurRepository;
    private final EnseignantRepository enseignantRepository;
    private final PasswordEncoder passwordEncoder;

    public EnseignantService(JournalisationService journalisationService, UtilisateurRepository utilisateurRepository,
                             EnseignantRepository enseignantRepository,PasswordEncoder passwordEncoder) {
        this.enseignantRepository = enseignantRepository;
        this.journalisationService = journalisationService;
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public ResponseEntity<?> registerEnseignant(Enseignant enseignantRequest) {
        try {
            if (enseignantRequest.getEmail() == null || enseignantRequest.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("L'email est obligatoire");
            }
            if (enseignantRequest.getPassword() == null || enseignantRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le mot de passe est obligatoire");
            }
            if (enseignantRequest.getNom() == null || enseignantRequest.getNom().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le nom est obligatoire");
            }
            if (enseignantRequest.getPrenom() == null || enseignantRequest.getPrenom().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le prénom est obligatoire");
            }
            if (enseignantRequest.getDepartement() == null || enseignantRequest.getDepartement().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le département est obligatoire");
            }

            Optional<Utilisateur> utilisateurExistant = utilisateurRepository.findByEmail(enseignantRequest.getEmail());
            if (utilisateurExistant.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Un utilisateur avec cet email existe déjà");
            }

            Enseignant nouvelEnseignant = new Enseignant(
                    enseignantRequest.getEmail(),
                    passwordEncoder.encode(enseignantRequest.getPassword()),
                    enseignantRequest.getPrenom(),
                    enseignantRequest.getNom(),
                    enseignantRequest.getDepartement(),
                    enseignantRequest.getDateNaissance()
            );

            Utilisateur saved = utilisateurRepository.save(nouvelEnseignant);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Compte enseignant créé avec succès");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la création du compte: " + e.getMessage());
        }
    }

    private Utilisateur getEnseignantCourant() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Utilisateur> u= utilisateurRepository.findByEmail(email);
        return u.orElse(null);
    }

    public void supprimerEnseignant(UUID id) {
        Enseignant enseignant = enseignantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enseignant non trouvé"));

        enseignantRepository.delete(enseignant);
    }

    public List<Enseignant> listerEnseignants() {
        return enseignantRepository.findAll();
    }

    public Optional<Enseignant> findByEmail(String email) {
        return enseignantRepository.findByEmail(email);
    }
}