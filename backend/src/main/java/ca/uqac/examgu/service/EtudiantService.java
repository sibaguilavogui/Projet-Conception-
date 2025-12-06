package ca.uqac.examgu.service;

import ca.uqac.examgu.dto.EtudiantDTO;
import ca.uqac.examgu.model.*;
import ca.uqac.examgu.model.Enumerations.Role;
import ca.uqac.examgu.model.Enumerations.StatutTentative;
import ca.uqac.examgu.model.Enumerations.TypeEvenement;
import ca.uqac.examgu.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EtudiantService {

    private final ExamenRepository examenRepo;
    private final JournalisationService journalisationService;
    private final UtilisateurRepository utilisateurRepository;
    private final EtudiantRepository etudiantRepository;
    private final EnseignantRepository enseignantRepository;
    private final InscriptionRepository inscriptionRepository;
    private final PasswordEncoder passwordEncoder;

    public EtudiantService(ExamenRepository examenRepo,  JournalisationService journalisationService,
                           UtilisateurRepository utilisateurRepository, EtudiantRepository etudiantRepository,
                           EnseignantRepository enseignantRepository, InscriptionRepository inscriptionRepository,
                           PasswordEncoder passwordEncoder) {
        this.examenRepo = examenRepo;
        this.journalisationService = journalisationService;
        this.utilisateurRepository = utilisateurRepository;
        this.etudiantRepository = etudiantRepository;
        this.enseignantRepository = enseignantRepository;
        this.inscriptionRepository = inscriptionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private Utilisateur getEtudiantCourant() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Utilisateur> u= utilisateurRepository.findByEmail(email);
        return u.orElse(null);
    }

    public List<Etudiant> listerEtudiants() {
        return etudiantRepository.findAll();
    }

    public Etudiant modifierEtudiant(UUID id, EtudiantDTO etudiantDTO) {
        Etudiant etudiant = etudiantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));

        etudiant.setNom(etudiantDTO.getNom());
        etudiant.setPrenom(etudiantDTO.getPrenom());
        etudiant.setPrenom(etudiantDTO.getPrenom());
        etudiant.setNom(etudiantDTO.getNom());
        etudiant.setDepartement(etudiantDTO.getDepartement());
        etudiant.setDateNaissance(etudiantDTO.getDateNaissance());

        Etudiant updated = etudiantRepository.save(etudiant);
        return updated;
    }

    public void supprimerEtudiant(UUID id) {
        Etudiant etudiant = etudiantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));

        List<Inscription> inscriptions = inscriptionRepository.findByEtudiantId(id);
        for (Inscription inscription : inscriptions) {
            inscription.suspendre();
            inscriptionRepository.save(inscription);
        }

        etudiantRepository.delete(etudiant);
    }

    public ResponseEntity<?> registerEtudiant(Etudiant etudiantRequest) {
        try {
            if (etudiantRequest.getEmail() == null || etudiantRequest.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("L'email est obligatoire");
            }
            if (etudiantRequest.getPassword() == null || etudiantRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le mot de passe est obligatoire");
            }
            if (etudiantRequest.getNom() == null || etudiantRequest.getNom().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le nom est obligatoire");
            }
            if (etudiantRequest.getPrenom() == null || etudiantRequest.getPrenom().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le prénom est obligatoire");
            }
            if (etudiantRequest.getDepartement() == null || etudiantRequest.getDepartement().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le département est obligatoire");
            }

            Optional<Utilisateur> utilisateurExistant = utilisateurRepository.findByEmail(etudiantRequest.getEmail());
            if (utilisateurExistant.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Un utilisateur avec cet email existe déjà");
            }

            Etudiant nouvelEtudiant = new Etudiant(
                    etudiantRequest.getEmail(),
                    passwordEncoder.encode(etudiantRequest.getPassword()),
                    etudiantRequest.getPrenom(),
                    etudiantRequest.getNom(),
                    etudiantRequest.getDepartement(),
                    etudiantRequest.getDateNaissance()
            );

            Utilisateur saved = utilisateurRepository.save(nouvelEtudiant);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Compte étudiant créé avec succès");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la création du compte: " + e.getMessage());
        }
    }


    public List<Inscription> getInscriptionsEtudiant(UUID etudiantId) {
        return inscriptionRepository.findByEtudiantId(etudiantId);
    }

    public Optional<Etudiant> findByEmail(String email) {
        return etudiantRepository.findByEmail(email);
    }
}