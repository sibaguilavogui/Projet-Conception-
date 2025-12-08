package ca.uqac.examgu.service;

import ca.uqac.examgu.model.*;
import ca.uqac.examgu.model.Enumerations.TypeEvenement;
import ca.uqac.examgu.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminService {

    private final UtilisateurRepository utilisateurRepository;
    private final LogEntryRepository logEntryRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UtilisateurRepository utilisateurRepository, LogEntryRepository logEntryRepository,
                        PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.logEntryRepository = logEntryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Admin getAdminSingleton() {
        return utilisateurRepository.findByEmail("admin@examgu.ca")
                .filter(utilisateur -> utilisateur instanceof Admin)
                .map(utilisateur -> (Admin) utilisateur)
                .orElseThrow(() -> new RuntimeException("Administrateur système non trouvé"));
    }

    public ResponseEntity<?> reinitialiserMotDePasseAdmin(String nouveauMotDePasse) {
        try {
            Admin admin = getAdminSingleton();
            admin.setPassword(passwordEncoder.encode(nouveauMotDePasse));
            utilisateurRepository.save(admin);
            return ResponseEntity.ok("Mot de passe administrateur réinitialisé avec succès");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la réinitialisation: " + e.getMessage());
        }
    }

    public List<LogEntry> consulterLogs() {
        return logEntryRepository.findAllByOrderByTimestampDesc();
    }

    public List<LogEntry> consulterLogsParUtilisateur(String email) {
        return logEntryRepository.findByUtilisateurOrderByTimestampDesc(email);
    }

    public List<LogEntry> consulterLogsParType(TypeEvenement type) {
        return logEntryRepository.findByTypeOrderByTimestampDesc(type);
    }

}