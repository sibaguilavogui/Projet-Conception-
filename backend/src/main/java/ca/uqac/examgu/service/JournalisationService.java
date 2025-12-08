package ca.uqac.examgu.service;

import ca.uqac.examgu.model.LogEntry;
import ca.uqac.examgu.model.Enumerations.TypeEvenement;
import ca.uqac.examgu.repository.LogEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JournalisationService {

    private static final Logger logger = LoggerFactory.getLogger("ExamGU");
    private final LogEntryRepository logEntryRepository;

    public JournalisationService(LogEntryRepository logEntryRepository) {
        this.logEntryRepository = logEntryRepository;
    }

    public void log(TypeEvenement type, String utilisateur, String details) {
        logger.info("[{}] Utilisateur: {} → {}", type, utilisateur, details);

        LogEntry logEntry = new LogEntry(type, utilisateur, details);

        logEntryRepository.save(logEntry);
    }

}