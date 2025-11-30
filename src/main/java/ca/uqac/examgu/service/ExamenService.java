package ca.uqac.examgu.service;

import ca.uqac.examgu.dto.ExamenDTO;
import ca.uqac.examgu.model.*;
import ca.uqac.examgu.model.Enumerations.EtatExamen;
import ca.uqac.examgu.model.Enumerations.TypeEvenement;
import ca.uqac.examgu.repository.ExamenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExamenService {

    private final ExamenRepository repo;
    private final JournalisationService journalisationService;

    public ExamenService(ExamenRepository repo, JournalisationService journalisationService) {
        this.repo = repo;
        this.journalisationService = journalisationService;
    }

    public Examen creer(ExamenDTO ex, Enseignant createur) {
        return repo.save(new Examen(ex.getTitre(), ex.getDescription(), ex.getDateDebut(), ex.getDateFin(),
                ex.getDureeMinutes(), createur));
    }

    public Examen modifierExamen(UUID examenId, ExamenDTO request, Enseignant enseignant) {
        Examen examen = repo.findById(examenId)
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

        if (request.isNotationAutomatique() != examen.isNotationAutomatique()) {
            examen.setNotationAutomatique(request.isNotationAutomatique());
        }

        return repo.save(examen);
    }

    public Optional<Examen> trouverParId(UUID id) {
        return repo.findById(id);
    }

    public Examen planifierExamen(UUID examenId, ExamenDTO request, Enseignant enseignant){
        Examen examen = repo.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));
        if ((request.getDateDebut() != null || request.getDateFin() != null || request.getDureeMinutes() != 0)) {
            try {
                examen.planifier(request.getDateDebut(), request.getDateFin(), request.getDureeMinutes());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Erreur dans la planification: " + e.getMessage());
            }

            return repo.save(examen);
        }
        throw new IllegalArgumentException("Erreur dans les données de planification");
    }

    public List<Examen> lister() {
        return repo.findAll();
    }
    public List<Examen> listerExamenEnseignant(Enseignant enseignant){return repo.findAll()
            .stream().filter(examen -> examen.getCreateur().equals(enseignant)).toList();}
    public List<Examen> listerExamenEtudiant(Etudiant etudiant){return repo.findAll()
            .stream().filter(examen -> examen.estInscrit(etudiant)).toList();}
}
