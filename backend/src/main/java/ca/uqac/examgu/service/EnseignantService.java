package ca.uqac.examgu.service;

import ca.uqac.examgu.model.*;
import ca.uqac.examgu.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EnseignantService {

    private final UtilisateurRepository utilisateurRepository;
    private final EnseignantRepository enseignantRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExamenRepository examenRepository;
    private final InscriptionRepository inscriptionRepository;
    private final TentativeRepository tentativeRepository;
    private final QuestionRepository questionRepository;

    public EnseignantService(UtilisateurRepository utilisateurRepository, EnseignantRepository enseignantRepository, PasswordEncoder passwordEncoder,
                             ExamenRepository examenRepository, InscriptionRepository inscriptionRepository,
                             TentativeRepository tentativeRepository, QuestionRepository questionRepository) {
        this.enseignantRepository = enseignantRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.examenRepository = examenRepository;
        this.inscriptionRepository = inscriptionRepository;
        this.tentativeRepository = tentativeRepository;
        this.questionRepository = questionRepository;
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

    public void supprimerEnseignant(UUID id) {
        Enseignant enseignant = enseignantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enseignant non trouvé"));
        List<Examen> examensCrees = examenRepository.findByCreateur(enseignant);

        if (!examensCrees.isEmpty()) {
            // Supprimer d'abord toutes les inscriptions liées à ces examens
            for (Examen examen : examensCrees) {
                inscriptionRepository.deleteByExamen(examen);
                // Supprimer aussi les questions, tentatives, etc.
                questionRepository.deleteByExamen(examen);
                tentativeRepository.deleteByExamen(examen);
            }

            // Ensuite supprimer les examens
            examenRepository.deleteAll(examensCrees);
        }
        enseignantRepository.delete(enseignant);
    }

    public List<Enseignant> listerEnseignants() {
        return enseignantRepository.findAll();
    }

    public Optional<Enseignant> findByEmail(String email) {
        return enseignantRepository.findByEmail(email);
    }
}