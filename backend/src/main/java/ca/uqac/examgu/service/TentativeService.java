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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        // Vérifier que l'enseignant est le créateur de l'examen
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

        if (!examen.getCreateur().getId().equals(enseignantId)) {
            throw new SecurityException("Seul le créateur de l'examen peut voir les tentatives");
        }

        return tentativeRepository.findByExamenId(examenId);
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

            } catch (Exception _) {

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