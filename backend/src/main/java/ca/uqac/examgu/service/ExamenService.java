package ca.uqac.examgu.service;

import ca.uqac.examgu.dto.ExamenDTO;
import ca.uqac.examgu.model.*;
import ca.uqac.examgu.model.Enumerations.EtatExamen;
import ca.uqac.examgu.model.Enumerations.TypeEvenement;
import ca.uqac.examgu.repository.ExamenRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ExamenService {

    private final ExamenRepository examenRepository;
    private final JournalisationService journalisationService;

    public ExamenService(ExamenRepository repo, ExamenRepository examenRepository, JournalisationService journalisationService) {
        this.examenRepository = examenRepository;
        this.journalisationService = journalisationService;
    }

    public Examen creer(ExamenDTO ex, Enseignant createur) {
        return examenRepository.save(createur.creerExamen(ex));
    }

    public Examen modifierExamen(UUID examenId, ExamenDTO request, Enseignant enseignant) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

        if (!examen.getCreateur().equals(enseignant)) {
            throw new SecurityException("Vous n'êtes pas le créateur de cet examen");
        }

        if (examen.getEtat() != EtatExamen.BROUILLON) {
            throw new IllegalStateException("Seuls les examens en brouillon peuvent être modifiés");
        }

        if (!Objects.equals(examen.getTitre(), request.getTitre())){
            examen.setTitre(request.getTitre());
        }

        if (!Objects.equals(examen.getDescription(), request.getDescription())) {
            examen.setDescription(request.getDescription());
        }

        if (!Objects.equals(examen.getDateDebut(), request.getDateDebut())) {
            examen.setDateDebut(request.getDateDebut());
        }

        if (!Objects.equals(examen.getDateFin(), request.getDateFin())) {
            examen.setDateFin(request.getDateFin());
        }

        if (request.getDureeMinutes() != examen.getDureeMinutes()) {
            examen.setDureeMinutes(request.getDureeMinutes());
        }

        return examenRepository.save(examen);
    }

    public Optional<Examen> trouverParId(UUID id) {
        return examenRepository.findById(id);
    }

    public Examen planifierExamen(UUID examenId, ExamenDTO request, Enseignant enseignant){
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));
        if ((request.getDateDebut() != null || request.getDateFin() != null || request.getDureeMinutes() != 0)) {
            try {
                examen.planifier(request.getDateDebut(), request.getDateFin(), request.getDureeMinutes());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Erreur dans la planification: " + e.getMessage());
            }

            return examenRepository.save(examen);
        }
        throw new IllegalArgumentException("Erreur dans les données de planification");
    }

    @Transactional
    public void supprimerExamen(UUID examenId, Enseignant enseignant) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

        // Vérifier que l'enseignant est le créateur
        if (!examen.getCreateur().equals(enseignant)) {
            throw new SecurityException("Vous n'êtes pas le créateur de cet examen");
        }

        // Vérifier que l'examen est en brouillon
        if (examen.getEtat() != EtatExamen.BROUILLON && examen.getEtat() != EtatExamen.PRET) {
            throw new IllegalStateException("Seuls les examens en brouillon peuvent être supprimés");
        }

        // Supprimer l'examen
        examenRepository.delete(examen);
    }

    public List<Inscription> listerInscriptionsEnseignant(UUID examenId, Enseignant enseignant) {
        Examen e = examenRepository.findById(examenId)
                .orElseThrow(()->new RuntimeException("Examen non trouvé"));
        if (!e.getCreateur().equals(enseignant)) {
            throw new SecurityException("Vous n'êtes pas le créateur de cet examen");
        }
        return e.getInscriptions();
    }

    public ResponseEntity<?> marquerExamenPret(Examen examen, Enseignant enseignant){
        if (!examen.getCreateur().equals(enseignant)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Seul le créateur de l'examen peut le marquer comme prêt");
        }

        // Valider que l'examen peut être marqué comme PRÊT
        List<String> validations = examen.getValidationsPourEtatPret();
        if (!validations.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Impossible de marquer l'examen comme PRÊT:\n" +
                            String.join("\n", validations));
        }

        // Marquer comme PRÊT
        examen.mettreEnEtatPret();
        examenRepository.save(examen);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Examen marqué comme PRÊT avec succès");
        response.put("examen", examen);
        return ResponseEntity.ok(response);
    }

    public List<Examen> lister() {
        return examenRepository.findAll();
    }
    public List<Examen> listerExamenEnseignant(Enseignant enseignant){return examenRepository.findAll()
            .stream().filter(examen -> examen.getCreateur().equals(enseignant)).toList();}

    public List<Examen> listerExamenEtudiant(Etudiant etudiant){return examenRepository.findAll()
            .stream().filter(examen -> examen.estInscrit(etudiant)).toList();}

    @Scheduled(fixedRate = 60000) // Vérifie toutes les minutes
    public void ouvrirExamensAutomatiquement() {
        LocalDateTime maintenant = LocalDateTime.now();

        List<Examen> examens = examenRepository.findAll();

        for (Examen examen : examens) {
            if (examen.getEtat() == EtatExamen.PRET
                    && examen.getDateDebut() != null
                    && !maintenant.isBefore(examen.getDateDebut())
                    && !maintenant.isAfter(examen.getDateFin())) {

                try {
                    examen.ouvrir();
                    examenRepository.save(examen);
                    System.out.println("Examen " + examen.getTitre() + " ouvert automatiquement");
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'ouverture de l'examen " + examen.getId() + ": " + e.getMessage());
                }
            }

            // Fermer les examens dont la date de fin est passée
            if (examen.getEtat() == EtatExamen.OUVERT
                    && examen.getDateFin() != null
                    && maintenant.isAfter(examen.getDateFin())) {

                examen.fermer();
                examenRepository.save(examen);
                System.out.println("Examen " + examen.getTitre() + " fermé automatiquement");
            }
        }
    }

}
