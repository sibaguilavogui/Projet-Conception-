package ca.uqac.examgu.service;

import ca.uqac.examgu.model.Etudiant;
import ca.uqac.examgu.model.Examen;
import ca.uqac.examgu.model.Tentative;
import ca.uqac.examgu.model.TypeEvenement;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TentativeService {

    // Stockage en mémoire pour le PoC
    private final Map<UUID, Tentative> tentatives = new ConcurrentHashMap<>();
    private final JournalisationService journalisationService;

    public TentativeService(JournalisationService journalisationService) {
        this.journalisationService = journalisationService;
    }

    public Tentative demarrer(Examen examen, Etudiant etudiant) {
        // Ici tu peux vérifier que l'examen est disponible
        if (!examen.estDisponible(LocalDateTime.now())) {
            throw new IllegalStateException("Examen non disponible");
        }

        Tentative tentative = new Tentative(examen, etudiant);
        tentative.demarrer(LocalDateTime.now());
        tentatives.put(tentative.getId(), tentative);

        journalisationService.log(
                TypeEvenement.DEMARRER_EXAMEN,
                etudiant.getEmail(),
                "Démarrage de l'examen " + examen.getTitre()
        );

        return tentative;
    }

    private Tentative getOrThrow(UUID tentativeId) {
        Tentative tentative = tentatives.get(tentativeId);
        if (tentative == null) {
            throw new IllegalArgumentException("Tentative introuvable : " + tentativeId);
        }
        return tentative;
    }

    public Tentative sauvegarderReponse(UUID tentativeId, UUID questionId, String contenu) {
        Tentative tentative = getOrThrow(tentativeId);

        tentative.sauvegarderReponse(questionId, contenu, LocalDateTime.now());

        journalisationService.log(
                TypeEvenement.SAUVEGARDE,
                tentative.getEtudiant().getEmail(),
                "Sauvegarde réponse pour question " + questionId
        );

        return tentative;
    }

    public Tentative soumettre(UUID tentativeId) {
        Tentative tentative = getOrThrow(tentativeId);

        tentative.soumettre(LocalDateTime.now());

        journalisationService.log(
                TypeEvenement.SOUMISSION,
                tentative.getEtudiant().getEmail(),
                "Soumission de l'examen " + tentative.getExamen().getTitre()
        );

        return tentative;
    }

    public int tempsRestant(UUID tentativeId) {
        Tentative tentative = getOrThrow(tentativeId);
        return tentative.tempsRestant(LocalDateTime.now());
    }
}


