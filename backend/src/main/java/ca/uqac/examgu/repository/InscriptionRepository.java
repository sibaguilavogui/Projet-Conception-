package ca.uqac.examgu.repository;

import ca.uqac.examgu.model.Examen;
import ca.uqac.examgu.model.Inscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InscriptionRepository extends JpaRepository<Inscription, Long> {
    List<Inscription> findByEtudiantId(UUID etudiantId);
    Optional<Inscription> findByExamenIdAndEtudiantId(UUID examenId, UUID etudiantId);

    void deleteByExamen(Examen examen);
}
