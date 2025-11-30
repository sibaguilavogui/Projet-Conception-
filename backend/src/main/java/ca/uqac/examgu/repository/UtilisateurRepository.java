package ca.uqac.examgu.repository;

import ca.uqac.examgu.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID>{
    Optional<Utilisateur> findByEmail(String email);
}
