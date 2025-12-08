package ca.uqac.examgu.service;

import ca.uqac.examgu.dto.ExamenDTO;
import ca.uqac.examgu.model.*;
import ca.uqac.examgu.model.Enumerations.EtatExamen;
import ca.uqac.examgu.model.Enumerations.StatutTentative;
import ca.uqac.examgu.repository.ExamenRepository;
import ca.uqac.examgu.repository.TentativeRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExamenService {

    private final ExamenRepository examenRepository;
    private final TentativeRepository tentativeRepository;

    public ExamenService(ExamenRepository examenRepository, TentativeRepository tentativeRepository) {
        this.examenRepository = examenRepository;
        this.tentativeRepository = tentativeRepository;
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

    public double getNoteEtudiant(UUID examenId, UUID etudiantId){
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));
        if(examen.isNotesVisibles()){
            Tentative tentative = tentativeRepository
                    .findByExamenIdAndEtudiantId(examenId, etudiantId)
                    .orElseThrow(()-> new RuntimeException("Tentative non trouvée"));
            if (!tentative.getEtudiant().getId().equals(etudiantId)) {
                throw new SecurityException("Accès non autorisé à cette tentative");
            }
            return tentative.getNoteFinale();
        }
        throw new RuntimeException("Resultat indisponible");
    }


    public Map<String, Object> corrigerAutomatiquement(UUID examenId) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

        List<Tentative> toutesTentatives = tentativeRepository.findByExamenId(examenId)
                .stream()
                .filter(t -> t.getStatut() == StatutTentative.SOUMISE)
                .collect(Collectors.toList());

        Map<String, Object> resultat = new HashMap<>();
        List<Map<String, Object>> tentativesCorrigees = new ArrayList<>();
        int qcmCorrigesTotal = 0;

        for (Tentative tentative : toutesTentatives) {
            Map<String, Object> infoTentative = new HashMap<>();
            infoTentative.put("tentativeId", tentative.getId());
            infoTentative.put("etudiant", tentative.getEtudiant().getNom() + " " + tentative.getEtudiant().getPrenom());

            int qcmCorriges = 0;
            for (ReponseDonnee reponse : tentative.getReponses()) {
                if (reponse.getQuestion().getType().equals("CHOIX") && !reponse.isEstCorrigee()) {
                    try {
                        reponse.corrigerAutomatiquement();
                        qcmCorriges++;
                    } catch (Exception e) {
                        // Ignorer les erreurs
                    }
                }
            }

            qcmCorrigesTotal += qcmCorriges;
            tentativeRepository.save(tentative);

            infoTentative.put("qcmCorriges", qcmCorriges);
            infoTentative.put("statut", "SUCCES");
            tentativesCorrigees.add(infoTentative);
        }

        resultat.put("examenId", examenId);
        resultat.put("titreExamen", examen.getTitre());
        resultat.put("tentativesCorrigees", tentativesCorrigees);
        resultat.put("qcmCorrigesTotal", qcmCorrigesTotal);
        resultat.put("message", String.format("%d QCM corrigés automatiquement dans %d tentatives",
                qcmCorrigesTotal, toutesTentatives.size()));

        return resultat;
    }


    public Examen publierNotes(UUID examenId){
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));
        if(estExamenTotalementCorrige(examenId, examen.getCreateur().getId())){
            examen.publierNotes();
            return examenRepository.save(examen);
        }

        throw new RuntimeException("Toute les tentatives ne sont pas corrigées");
    }

    public Examen masquerNotes(UUID examenId){
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));
        examen.masquerNotes();
        return examenRepository.save(examen);
    }


    public boolean estExamenTotalementCorrige(UUID examenId, UUID enseignantId) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

        if (!examen.getCreateur().getId().equals(enseignantId)) {
            throw new SecurityException("Seul le créateur de l'examen peut voir l'état de correction");
        }

        List<Tentative> tentatives = tentativeRepository.findByExamenId(examenId);

        if (tentatives.isEmpty()) {
            return true; // Aucune tentative, donc rien à corriger
        }

        return tentatives.stream().allMatch(Tentative::isEstCorrigee);
    }

    public Map<String, Object> calculerNotesFinalesExamen(UUID examenId) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

        List<Tentative> toutesTentatives = tentativeRepository.findByExamenId(examenId)
                .stream()
                .filter(t -> t.getStatut() == StatutTentative.SOUMISE)
                .collect(Collectors.toList());

        Map<String, Object> resultat = new HashMap<>();
        List<Map<String, Object>> tentativesTraitees = new ArrayList<>();
        List<Map<String, Object>> tentativesAvecProblemes = new ArrayList<>();
        boolean toutesCorrigees = true;

        for (Tentative tentative : toutesTentatives) {
            Map<String, Object> infoTentative = new HashMap<>();
            infoTentative.put("tentativeId", tentative.getId());
            infoTentative.put("etudiant", tentative.getEtudiant().getNom() + " " + tentative.getEtudiant().getPrenom());

            // Vérifier les questions à développement non corrigées
            List<Question> questionsDevNonCorrigees = tentative.getReponses().stream()
                    .filter(r -> r.getQuestion().getType().equals("DEVELOPPEMENT") && !r.isEstCorrigee())
                    .map(ReponseDonnee::getQuestion)
                    .collect(Collectors.toList());

            if (!questionsDevNonCorrigees.isEmpty()) {
                toutesCorrigees = false;
                infoTentative.put("statut", "ERREUR");
                infoTentative.put("message", "Questions à développement non corrigées");
                infoTentative.put("questionsNonCorrigees", questionsDevNonCorrigees.stream()
                        .map(q -> Map.of(
                                "id", q.getId(),
                                "enonce", q.getEnonce().substring(0, Math.min(50, q.getEnonce().length())) + "..."
                        ))
                        .collect(Collectors.toList()));
                tentativesAvecProblemes.add(infoTentative);
                continue;
            }

            // Corriger automatiquement les QCM
            int qcmCorriges = 0;
            for (ReponseDonnee reponse : tentative.getReponses()) {
                if (reponse.getQuestion().getType().equals("CHOIX") && !reponse.isEstCorrigee()) {
                    try {
                        reponse.corrigerAutomatiquement();
                        qcmCorriges++;
                    } catch (Exception e) {
                        // Ignorer les erreurs pour cette correction
                    }
                }
            }

            // Calculer la note finale
            tentative.calculerNoteFinale();
            tentativeRepository.save(tentative);

            infoTentative.put("statut", "SUCCES");
            infoTentative.put("qcmCorriges", qcmCorriges);
            infoTentative.put("noteFinale", tentative.getNoteFinale());
            tentativesTraitees.add(infoTentative);
        }

        resultat.put("examenId", examenId);
        resultat.put("titreExamen", examen.getTitre());
        resultat.put("tentativesTraitees", tentativesTraitees);
        resultat.put("tentativesAvecProblemes", tentativesAvecProblemes);
        resultat.put("toutesCorrigees", toutesCorrigees);

        if (toutesCorrigees) {
            resultat.put("message", "Toutes les notes ont été calculées avec succès");
            // Marquer l'examen comme corrigé si vous avez cet attribut
            // examen.setCorrige(true);
            // examenRepository.save(examen);
        } else {
            resultat.put("message", "Certaines tentatives n'ont pas pu être entièrement corrigées");
        }

        return resultat;
    }

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