package ca.uqac.examgu.service;

import ca.uqac.examgu.model.EtatExamen;
import ca.uqac.examgu.model.Examen;
import ca.uqac.examgu.repository.ExamenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExamenService {

    private final ExamenRepository repo;

    public ExamenService(ExamenRepository repo) {
        this.repo = repo;
    }

    public Examen creer(Examen ex) {
        ex.setEtat(EtatExamen.BROUILLON);
        return repo.save(ex);
    }

    public boolean estDisponible(Examen ex) {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(ex.getDateDebut()) && now.isBefore(ex.getDateFin());
    }

    public List<Examen> lister() {
        return repo.findAll();
    }
}
