package ca.uqac.examgu.repository;

import ca.uqac.examgu.model.ReponseDonnee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReponseDonneeRepository extends JpaRepository<ReponseDonnee, Long> {
    List<ReponseDonnee> findByTentativeId(UUID tentativeId);
}
