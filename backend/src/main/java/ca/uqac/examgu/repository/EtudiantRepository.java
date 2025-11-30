package ca.uqac.examgu.repository;

import ca.uqac.examgu.model.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EtudiantRepository extends JpaRepository<Etudiant, UUID> {
    Optional<Etudiant> findByEmail(String name);
}