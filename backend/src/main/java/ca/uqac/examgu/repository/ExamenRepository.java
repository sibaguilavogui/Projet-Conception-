package ca.uqac.examgu.repository;

import ca.uqac.examgu.model.Enseignant;
import ca.uqac.examgu.model.Examen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamenRepository extends JpaRepository<Examen, UUID> {

    Optional<Examen> findById(UUID examenId);

    List<Examen> findByCreateur(Enseignant enseignant);
}
