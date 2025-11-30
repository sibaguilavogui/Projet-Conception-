package ca.uqac.examgu.repository;

import ca.uqac.examgu.model.LogEntry;
import ca.uqac.examgu.model.Enumerations.TypeEvenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LogEntryRepository extends JpaRepository<LogEntry, UUID> {
    List<LogEntry> findAllByOrderByTimestampDesc();
    List<LogEntry> findByUtilisateurOrderByTimestampDesc(String utilisateur);
    List<LogEntry> findByTypeOrderByTimestampDesc(TypeEvenement type);

}