package ca.uqac.examgu.repository;

import ca.uqac.examgu.model.Inscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InscriptionRepository extends JpaRepository<Inscription, Long> {
    List<Inscription> findByEtudiantId(UUID etudiantId);
    List<Inscription> findByExamenId(UUID examenId);
}
