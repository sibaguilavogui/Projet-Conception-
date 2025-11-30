package ca.uqac.examgu.service;

import ca.uqac.examgu.dto.QuestionAChoixDTO;
import ca.uqac.examgu.dto.QuestionADeveloppementDTO;
import ca.uqac.examgu.model.*;
import ca.uqac.examgu.repository.ExamenRepository;
import ca.uqac.examgu.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QuestionService {

    private final ExamenRepository examenRepository;
    private final QuestionRepository questionRepository;

    public QuestionService(ExamenRepository examenRepository, QuestionRepository questionRepository) {
        this.examenRepository = examenRepository;
        this.questionRepository = questionRepository;
    }

    public Examen ajouterQuestionChoix(QuestionAChoixDTO request, Examen examen) {
        examen.ajouterQuestionAChoix(request.getEnonce(), request.getBareme(),
                request.getTypeChoix(), request.getNombreChoixMin(), request.getNombreChoixMax());

        return examenRepository.save(examen);
    }

    public Examen ajouterQuestionDeveloppement(QuestionADeveloppementDTO request, Examen examen) {
        examen.ajouterQuestionADeveloppement(request.getEnonce(), request.getBareme());
        return examenRepository.save(examen);
    }

    public boolean supprimerQuestion(UUID questionId, Examen examen) {
        Question question = questionRepository.findById(questionId).
                orElseThrow(() -> new RuntimeException("Question non trouvée"));
        if(examen.getQuestions().contains(question)){
            examen.retirerQuestion(questionId);
            return true;
        }

        return false;
    }


}