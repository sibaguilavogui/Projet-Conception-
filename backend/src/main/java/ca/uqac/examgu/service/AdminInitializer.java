package ca.uqac.examgu.service;

import ca.uqac.examgu.model.Admin;
import ca.uqac.examgu.repository.UtilisateurRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UtilisateurRepository utilisateurRepository,
                            PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (utilisateurRepository.findByEmail("admin@examgu.ca").isEmpty()) {
            Admin admin = new Admin(
                    "admin@examgu.ca",
                    passwordEncoder.encode("admin123"),
                    "System",
                    "Administrator",
                    LocalDate.of(2025, 12, 9)
            );

            utilisateurRepository.save(admin);

            System.out.println("=========================================");
            System.out.println("ADMINISTRATEUR SYSTÈME CRÉÉ AVEC SUCCÈS");
            System.out.println("Email: admin@examgu.ca");
            System.out.println("Mot de passe: admin123");
            System.out.println("=========================================");
        } else {
            System.out.println("Administrateur système déjà existant");
        }
    }
}