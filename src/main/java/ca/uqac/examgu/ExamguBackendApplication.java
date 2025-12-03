package ca.uqac.examgu;

import ca.uqac.examgu.application.ExamGuSystem;
import ca.uqac.examgu.domain.Admin;
import ca.uqac.examgu.domain.Enseignant;
import ca.uqac.examgu.domain.Etudiant;
import ca.uqac.examgu.domain.Examen;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.LocalDateTime;

@SpringBootApplication
public class ExamguBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExamguBackendApplication.class, args);
	}

	@Bean
	public ExamGuSystem examGuSystem() {
		ExamGuSystem sys = new ExamGuSystem();

		// 1) Admin par défaut
		Admin admin = new Admin(
				"admin@uqac.ca",
				"admin",
				"Admin",
				"Système",
				"INF",
				LocalDate.of(1990, 1, 1)
		);
		sys.creerUtilisateurBootstrapSilencieux(admin);

		// 2) Enseignant de test
		Enseignant ens = new Enseignant(
				"prof1@uqac.ca",
				"secret",
				"Prof",
				"Demo",
				"INF",
				LocalDate.of(1980, 1, 1)
		);
		sys.creerUtilisateurBootstrapSilencieux(ens);

		// 3) Étudiant de test
		Etudiant etu1 = new Etudiant(
				"etu1@uqac.ca",
				"1234",
				"Etu",
				"Test",
				"INF",
				LocalDate.of(2002, 1, 1)
		);
		sys.creerUtilisateurBootstrapSilencieux(etu1);

		// 4) Examen de test
		Examen ex = sys.creerExamen(ens, "Examen intra GEI311");

		// 5) Inscrire l’étudiant à l’examen (par l’admin)
		sys.inscrire(admin, ex.getId(), etu1.getId());

		// 6) Planifier + ouvrir l’examen pour maintenant
		LocalDateTime debut = LocalDateTime.now().minusMinutes(5);
		LocalDateTime fin   = LocalDateTime.now().plusHours(2);
		sys.planifierEtOuvrirExamen(ens, ex.getId(), debut, fin, 120);

		return sys;
	}
}
