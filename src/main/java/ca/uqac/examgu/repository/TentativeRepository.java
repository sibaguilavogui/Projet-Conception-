package ca.uqac.examgu.repository;

import ca.uqac.examgu.model.Tentative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TentativeRepository extends JpaRepository<Tentative, Long> {
    List<Tentative> findByEtudiantId(UUID etudiantId);
    List<Tentative> findByExamenId(UUID examenId);

}