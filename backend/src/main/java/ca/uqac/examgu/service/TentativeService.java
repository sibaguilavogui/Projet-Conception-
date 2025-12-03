package ca.uqac.examgu.service;

import ca.uqac.examgu.model.*;
import ca.uqac.examgu.model.Enumerations.StatutTentative;
import ca.uqac.examgu.repository.EtudiantRepository;
import ca.uqac.examgu.repository.TentativeRepository;
import ca.uqac.examgu.repository.ExamenRepository;
import ca.uqac.examgu.repository.UtilisateurRepository;
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

        if (!examen.estOuvert()) {
            throw new IllegalStateException("L'examen n'est pas disponible en ce moment");
        }

        Optional<Tentative> tentativeExistante = tentativeRepository
                .findByExamenIdAndEtudiantId(examenId, etudiantId);

        if (tentativeExistante.isPresent()) {
            Tentative tentative = tentativeExistante.get();
            if (tentative.getStatut() == StatutTentative.EN_COURS && !tentative.estExpiree()) {
                return tentative;
            }
        }

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

        tentative.sauvegarderReponse(questionId, contenu, LocalDateTime.now());

        tentative.setDateModification(LocalDateTime.now());
        return tentativeRepository.save(tentative);
    }

    public Tentative soumettreTentative(UUID tentativeId, UUID etudiantId) {
        Tentative tentative = tentativeRepository.findById(tentativeId)
                .orElseThrow(() -> new RuntimeException("Tentative non trouvée"));

        if (!tentative.getEtudiant().getId().equals(etudiantId)) {
            throw new SecurityException("Accès non autorisé à cette tentative");
        }

        tentative.soumettre(LocalDateTime.now());

        return tentativeRepository.save(tentative);
    }

    public Tentative reprendreTentative(UUID tentativeId, UUID etudiantId) {
        Tentative tentative = tentativeRepository.findById(tentativeId)
                .orElseThrow(() -> new RuntimeException("Tentative non trouvée"));

        if (!tentative.getEtudiant().getId().equals(etudiantId)) {
            throw new SecurityException("Accès non autorisé à cette tentative");
        }

        if (tentative.getStatut() != StatutTentative.EN_COURS) {
            throw new IllegalStateException("La tentative ne peut pas être reprise");
        }

        if (tentative.estExpiree()) {
            tentative.soumettre(LocalDateTime.now());
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

    public List<Tentative> getTentativesACorriger(UUID examenId, UUID enseignantId) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

        if (!examen.getCreateur().getId().equals(enseignantId)) {
            throw new SecurityException("Seul le créateur de l'examen peut voir les tentatives à corriger");
        }

        List<Tentative> toutesTentatives = tentativeRepository.findByExamenId(examenId);

        return toutesTentatives.stream()
                .filter(tentative -> {
                    if (tentative.getStatut() != StatutTentative.SOUMISE &&
                            tentative.getStatut() != StatutTentative.EXPIREE) {
                        return false;
                    }

                    if (tentative.isEstCorrigee()) {
                        return false;
                    }

                    return tentative.getReponses().stream()
                            .anyMatch(reponse -> {
                                if (reponse.estQuestionDeveloppement() && !reponse.isEstCorrigee()) {
                                    return true;
                                }
                                return false;
                            });
                })
                .sorted(Comparator.comparing(Tentative::getDateSoumission))
                .collect(Collectors.toList());
    }

    public Tentative getTentativePourCorrection(UUID tentativeId, UUID enseignantId) {
        Tentative tentative = tentativeRepository.findById(tentativeId)
                .orElseThrow(() -> new RuntimeException("Tentative non trouvée"));

        Examen examen = tentative.getExamen();
        if (!examen.getCreateur().getId().equals(enseignantId)) {
            throw new SecurityException("Accès non autorisé à cette tentative");
        }

        if (tentative.getStatut() != StatutTentative.SOUMISE &&
                tentative.getStatut() != StatutTentative.EXPIREE) {
            throw new IllegalStateException("Cette tentative ne peut pas être corrigée");
        }

        return tentative;
    }

    public ReponseDonnee corrigerQuestionDeveloppement(UUID tentativeId, UUID reponseId,
                                                       double note, String commentaire,
                                                       UUID enseignantId) {
        Tentative tentative = getTentativePourCorrection(tentativeId, enseignantId);

        ReponseDonnee reponse = tentative.getReponses().stream()
                .filter(r -> r.getId().equals(reponseId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Réponse non trouvée"));

        if (!reponse.estQuestionDeveloppement()) {
            throw new IllegalArgumentException("Seules les questions à développement peuvent être corrigées manuellement");
        }

        double bareme = reponse.getQuestion().getBareme();
        if (note > bareme) {
            throw new IllegalArgumentException(
                    String.format("La note (%.1f) dépasse le barème de la question (%.1f)", note, bareme));
        }

        if (note < 0) {
            throw new IllegalArgumentException("La note ne peut pas être négative");
        }

        reponse.noterPatiellement(note, commentaire, false);

        tentative.recalculerScoreTotal();

        // Si toutes les questions sont corrigées, marquer la tentative comme corrigée
        boolean toutesCorrigees = tentative.getReponses().stream()
                .allMatch(ReponseDonnee::isEstCorrigee);

        if (toutesCorrigees) {
            tentative.setCorrigee(true);
        }

        tentativeRepository.save(tentative);

        return reponse;
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

    public ReponseDonnee annulerCorrectionReponse(UUID tentativeId, UUID reponseId, UUID enseignantId) {
        Tentative tentative = getTentativePourCorrection(tentativeId, enseignantId);

        ReponseDonnee reponse = tentative.getReponses().stream()
                .filter(r -> r.getId().equals(reponseId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Réponse non trouvée"));

        reponse.annulerCorrection();

        tentative.recalculerScoreTotal();
        tentative.setCorrigee(false);

        tentativeRepository.save(tentative);

        return reponse;
    }


    @Scheduled(fixedRate = 30000) // 30 secondes
    @Transactional
    public void sauvegarderTentativesEnCours() {
        LocalDateTime maintenant = LocalDateTime.now();

        List<Tentative> tentativesEnCours = tentativeRepository
                .findByStatutAndDebutAfter(StatutTentative.EN_COURS, maintenant.minusHours(4));

        for (Tentative tentative : tentativesEnCours) {
            try {
                if (tentative.estExpiree(maintenant)) {
                    tentative.soumettre(maintenant);
                }

                tentativeRepository.save(tentative);

            } catch (Exception e) {
                return;
            }
        }

        if (!tentativesEnCours.isEmpty()) {
            System.out.println("Sauvegarde auto: " + tentativesEnCours.size() + " tentatives traitées");
        }
    }


    @Scheduled(cron = "0 0 2 * * ?") // Tous les jours à 2h du matin
    @Transactional
    public void nettoyerTentativesExpirees() {
        LocalDateTime maintenant = LocalDateTime.now();
        List<Tentative> tentativesExpirees = tentativeRepository
                .findExpiredTentatives(StatutTentative.EN_COURS, maintenant);

        for (Tentative tentative : tentativesExpirees) {
            try {
                tentative.soumettre(maintenant);
                tentativeRepository.save(tentative);
            } catch (Exception e) {
                return;
            }
        }

        if (!tentativesExpirees.isEmpty()) {
            System.out.println("Nettoyage: " + tentativesExpirees.size() + " tentatives expirées soumises");
        }
    }


}