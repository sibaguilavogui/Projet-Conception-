package ca.uqac.examgu.repository;

import ca.uqac.examgu.model.ResultatExamen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResultatExamenRepository extends JpaRepository<ResultatExamen, UUID> {
    List<ResultatExamen> findByEtudiantId(UUID etudiantId);
    List<ResultatExamen> findByExamenId(UUID examenId);
    List<ResultatExamen> findByEtudiantIdAndVisibleTrue(UUID etudiantId);
    ResultatExamen findByExamenIdAndEtudiantId(UUID examenId, UUID etudiantId);
}