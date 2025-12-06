package ca.uqac.examgu.repository;

import ca.uqac.examgu.model.Examen;
import ca.uqac.examgu.model.Tentative;
import ca.uqac.examgu.model.Enumerations.StatutTentative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TentativeRepository extends JpaRepository<Tentative, UUID> {

    Optional<Tentative> findByExamenIdAndEtudiantId(UUID examenId, UUID etudiantId);

    List<Tentative> findByExamenId(UUID examenId);

    List<Tentative> findByStatut(StatutTentative statut);

    @Query("SELECT t FROM Tentative t WHERE t.statut = :statut AND t.fin < :now")
    List<Tentative> findExpiredTentatives(@Param("statut") StatutTentative statut,
                                          @Param("now") LocalDateTime now);

    void deleteByExamen(Examen examen);
}