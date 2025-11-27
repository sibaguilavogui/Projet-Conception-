package ca.uqac.examgu.service;

import ca.uqac.examgu.model.TypeEvenement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JournalisationService {

    private static final Logger logger = LoggerFactory.getLogger("ExamGU");

    public void log(TypeEvenement type, String utilisateur, String details) {
        logger.info("[{}] Utilisateur: {} → {}", type, utilisateur, details);
    }
}
