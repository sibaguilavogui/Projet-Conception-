package ca.uqac.examgu.service;

import ca.uqac.examgu.model.*;
import ca.uqac.examgu.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InscriptionService {

    private final EtudiantRepository etudiantRepository;
    private final ExamenRepository examenRepository;
    private final InscriptionRepository inscriptionRepository;

    public InscriptionService(EtudiantRepository etudiantRepository,
                        ExamenRepository examenRepository,
                        InscriptionRepository inscriptionRepository) {
        this.etudiantRepository = etudiantRepository;
        this.examenRepository = examenRepository;
        this.inscriptionRepository = inscriptionRepository;
    }

    public Inscription inscrireEtudiant(UUID examenId, UUID etudiantId) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));

        return inscriptionRepository.save(examen.inscrireEtudiant(etudiant));
    }

    public void desinscrireEtudiant(UUID examenId, UUID etudiantId) {
        Inscription inscription = inscriptionRepository
                .findByExamenIdAndEtudiantId(examenId, etudiantId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));

        inscriptionRepository.delete(inscription);
    }

    public List<Inscription> getInscriptions(){
        return inscriptionRepository.findAll();
    }

}