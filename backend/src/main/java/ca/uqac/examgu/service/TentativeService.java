package ca.uqac.examgu.service;

import ca.uqac.examgu.model.*;
import ca.uqac.examgu.model.Enumerations.StatutTentative;
import ca.uqac.examgu.repository.EtudiantRepository;
import ca.uqac.examgu.repository.TentativeRepository;
import ca.uqac.examgu.repository.ExamenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TentativeService {

    private final TentativeRepository tentativeRepository;
    private final ExamenRepository examenRepository;
    private final EtudiantRepository etudiantRepository;

    public TentativeService(TentativeRepository tentativeRepository,
                            ExamenRepository examenRepository,
                            EtudiantRepository etudiantRepository) {
        this.tentativeRepository = tentativeRepository;
        this.examenRepository = examenRepository;
        this.etudiantRepository = etudiantRepository;
    }

    public Tentative demarrerTentative(UUID examenId, UUID etudiantId) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Etudiant non trouvé"));

        if (!examen.estInscrit(etudiant)) {
            throw new SecurityException("L'étudiant n'est pas inscrit à cet examen");
        }

        if (!examen.estDisponible()) {
            throw new IllegalStateException("L'examen n'est pas disponible en ce moment");
        }

        // Chercher une tentative existante pour cet étudiant et cet examen
        Optional<Tentative> tentativeExistante = tentativeRepository
                .findByExamenIdAndEtudiantId(examenId, etudiantId);

        if (tentativeExistante.isPresent()) {
            Tentative tentative = tentativeExistante.get();

            // Si la tentative est en cours et la deadline n'est pas atteinte
            if (tentative.getStatut() == StatutTentative.EN_COURS && !tentative.estExpiree()) {
                return tentative; // Retourner la tentative existante avec ses réponses sauvegardées
            }

            // Si la tentative est en cours mais expirée, ne pas la soumettre automatiquement
            // Laissez l'étudiant voir qu'elle est expirée
            if (tentative.getStatut() == StatutTentative.EN_COURS && tentative.estExpiree()) {
                // Retourner la tentative expirée sans la soumettre
                // L'étudiant pourra voir qu'elle est expirée mais pas la modifier
                return tentative;
            }

            // Si la tentative est déjà soumise, la retourner
            if (tentative.getStatut() == StatutTentative.SOUMISE) {
                return tentative;
            }
        }

        // Créer une nouvelle tentative
        Tentative nouvelleTentative = new Tentative(examen, etudiant);
        nouvelleTentative.demarrer();
        return tentativeRepository.save(nouvelleTentative);
    }

    public Tentative sauvegarderReponse(UUID tentativeId, UUID questionId, String contenu, UUID etudiantId) {
        Tentative tentative = tentativeRepository.findById(tentativeId)
                .orElseThrow(() -> new RuntimeException("Tentative non trouvée"));

        if (!tentative.getEtudiant().getId().equals(etudiantId)) {
            throw new SecurityException("Accès non autorisé à cette tentative");
        }

        if (!tentative.getStatut().equals(StatutTentative.EN_COURS)) {
            throw new IllegalStateException("La tentative ne peut plus être modifiée");
        }

        tentative.sauvegarderReponse(questionId, contenu);

        return tentativeRepository.save(tentative);
    }

    public Tentative soumettreTentative(UUID tentativeId, UUID etudiantId) throws Exception {
        Tentative tentative = tentativeRepository.findById(tentativeId)
                .orElseThrow(() -> new RuntimeException("Tentative non trouvée"));

        if (!tentative.getEtudiant().getId().equals(etudiantId)) {
            throw new SecurityException("Accès non autorisé à cette tentative");
        }

        tentative.soumettre();

        return tentativeRepository.save(tentative);
    }


    public Tentative reprendreTentative(UUID tentativeId, UUID etudiantId) throws Exception {
        Tentative tentative = tentativeRepository.findById(tentativeId)
                .orElseThrow(() -> new RuntimeException("Tentative non trouvée"));

        if (!tentative.getEtudiant().getId().equals(etudiantId)) {
            throw new SecurityException("Accès non autorisé à cette tentative");
        }

        if (tentative.getStatut() != StatutTentative.EN_COURS) {
            throw new IllegalStateException("La tentative ne peut pas être reprise");
        }

        if (tentative.estExpiree()) {
            tentative.soumettre();
            throw new IllegalStateException("La tentative était expirée et a été soumise automatiquement");
        }
        return tentativeRepository.save(tentative);
    }

    public Tentative getTentative(UUID tentativeId, UUID etudiantId) {
        Tentative tentative = tentativeRepository.findById(tentativeId)
                .orElseThrow(() -> new RuntimeException("Tentative non trouvée"));

        if (!tentative.getEtudiant().getId().equals(etudiantId)) {
            throw new SecurityException("Accès non autorisé à cette tentative");
        }

        return tentative;
    }

    public List<Tentative> getTentativesExamen(UUID examenId, UUID enseignantId) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

        if (!examen.getCreateur().getId().equals(enseignantId)) {
            throw new SecurityException("Seul le créateur de l'examen peut voir les tentatives");
        }

        return tentativeRepository.findByExamenId(examenId);
    }

    public Tentative getTentativePourCorrection(UUID tentativeId, UUID enseignantId) {
        Tentative tentative = tentativeRepository.findById(tentativeId)
                .orElseThrow(() -> new RuntimeException("Tentative non trouvée"));

        Examen examen = tentative.getExamen();
        if (!examen.getCreateur().getId().equals(enseignantId)) {
            throw new SecurityException("Accès non autorisé à cette tentative");
        }

        if (tentative.getStatut() != StatutTentative.SOUMISE) {
            throw new IllegalStateException("Cette tentative ne peut pas être corrigée");
        }

        return tentative;
    }


    public List<ReponseDonnee> getReponsesDevTentative(UUID tentativeId, UUID enseignantId) {
        Tentative tentative = tentativeRepository.findById(tentativeId)
                .orElseThrow(() -> new RuntimeException("Tentative non trouvée"));

        Examen examen = tentative.getExamen();
        if (!examen.getCreateur().getId().equals(enseignantId)) {
            throw new SecurityException("Accès non autorisé à cette tentative");
        }

        if (tentative.getStatut() != StatutTentative.SOUMISE) {
            throw new IllegalStateException("Cette tentative ne peut pas être corrigée");
        }

        // Filtrer les réponses pour ne garder que les questions à développement
        List<ReponseDonnee> reponsesDevOnly = tentative.getReponses().stream()
                .filter(reponse -> reponse.getQuestion().getType().equals("DEVELOPPEMENT"))
                .collect(Collectors.toList());

        return reponsesDevOnly;
    }


    public Tentative corrigerQuestionDeveloppement(UUID tentativeId, UUID reponseId,
                                                   double note, String commentaire,
                                                   UUID enseignantId) {
        Tentative tentative = getTentativePourCorrection(tentativeId, enseignantId);

        ReponseDonnee reponse = tentative.getReponses().stream()
                .filter(r -> r.getId().equals(reponseId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Réponse non trouvée"));

        if (!reponse.getQuestion().getType().equals("DEVELOPPEMENT")) {
            throw new IllegalArgumentException("Seules les questions à développement peuvent être corrigées manuellement");
        }

        reponse.noterPatiellement(note, commentaire);

        // Si toutes les questions sont corrigées, marquer la tentative comme corrigée
        boolean toutesCorrigees = tentative.getReponses().stream()
                .allMatch(ReponseDonnee::isEstCorrigee);

        if (toutesCorrigees) {
            tentative.setCorrigee(true);
        }

        return tentativeRepository.save(tentative);
    }

    public Tentative corrigerTentativeComplete(UUID tentativeId, Map<UUID, Double> notes,
                                               Map<UUID, String> commentaires, UUID enseignantId) {
        Tentative tentative = getTentativePourCorrection(tentativeId, enseignantId);

        for (Map.Entry<UUID, Double> entry : notes.entrySet()) {
            UUID reponseId = entry.getKey();
            double note = entry.getValue();
            String commentaire = commentaires.get(reponseId);

            corrigerQuestionDeveloppement(tentativeId, reponseId, note, commentaire, enseignantId);
        }

        tentative.setCorrigee(true);

        return tentativeRepository.save(tentative);
    }


    @Scheduled(fixedRate = 30000) // Toutes les 30 secondes
    @Transactional
    public void sauvegarderTentativesEnCours() {
        LocalDateTime maintenant = LocalDateTime.now();

        List<Tentative> tentativesEnCours = tentativeRepository
                .findByStatut(StatutTentative.EN_COURS);

        for (Tentative tentative : tentativesEnCours) {
            try {
                // Sauvegarder la tentative
                tentativeRepository.save(tentative);

                // Vérifier si la deadline est atteinte
                if (tentative.estExpiree()) {
                    // Soumettre automatiquement la tentative
                    tentative.soumettre();
                    tentativeRepository.save(tentative);
                    System.out.println("Tentative " + tentative.getId() + " soumise automatiquement (deadline atteinte)");
                }

            } catch (Exception e) {
                System.err.println("Erreur lors de la sauvegarde de la tentative " + tentative.getId() + ": " + e.getMessage());
            }
        }

        if (!tentativesEnCours.isEmpty()) {
            System.out.println("Sauvegarde auto: " + tentativesEnCours.size() + " tentatives traitées");
        }
    }

}