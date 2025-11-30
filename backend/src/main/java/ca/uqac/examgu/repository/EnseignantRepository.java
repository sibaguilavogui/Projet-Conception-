package ca.uqac.examgu.repository;

import ca.uqac.examgu.model.Enseignant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EnseignantRepository extends JpaRepository<Enseignant, UUID> {
    Optional<Enseignant> findByEmail(String email);
}