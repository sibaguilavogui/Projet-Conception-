package ca.uqac.examgu.service;

import ca.uqac.examgu.dto.QuestionAChoixDTO;
import ca.uqac.examgu.dto.QuestionADeveloppementDTO;
import ca.uqac.examgu.dto.ReponsePossibleDTO;
import ca.uqac.examgu.model.*;
import ca.uqac.examgu.model.Enumerations.PolitiqueCorrectionQCM;
import ca.uqac.examgu.repository.ExamenRepository;
import ca.uqac.examgu.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final ExamenRepository examenRepository;
    private final QuestionRepository questionRepository;

    public QuestionService(ExamenRepository examenRepository, QuestionRepository questionRepository) {
        this.examenRepository = examenRepository;
        this.questionRepository = questionRepository;
    }

    public Examen ajouterQuestionChoix(QuestionAChoixDTO request, Examen examen) {
        List<ReponsePossibleDTO> optionsDTO = request.getOptions();
        if (optionsDTO == null || optionsDTO.size() < 2) {
            throw new IllegalArgumentException("Une question à choix doit avoir au moins 2 options");
        }

        // Convertir les DTOs en entités ReponsePossible
        List<ReponsePossible> options = optionsDTO.stream()
                .map(dto -> new ReponsePossible(dto.getLibelle(), dto.isCorrecte()))
                .collect(Collectors.toList());

        // Compter les options correctes
        long nbOptionsCorrectes = options.stream()
                .filter(ReponsePossible::isCorrecte)
                .count();

        // Valider selon le type de question
        if (request.getTypeChoix() == QuestionAChoix.TypeChoix.UNIQUE) {
            // Pour UNIQUE, vérifier qu'il y a exactement une bonne réponse
            if (nbOptionsCorrectes != 1) {
                throw new IllegalArgumentException(
                        String.format("Une question à choix unique doit avoir exactement une réponse correcte (trouvé: %d)",
                                nbOptionsCorrectes)
                );
            }
            // Pour UNIQUE, forcer nombreChoixMin et nombreChoixMax à 1
            request.setNombreChoixMin(1);
            request.setNombreChoixMax(1);
        } else {
            // Pour QCM, vérifier qu'il y a au moins une bonne réponse
            if (nbOptionsCorrectes == 0) {
                throw new IllegalArgumentException("Une question QCM doit avoir au moins une réponse correcte");
            }

            // Pour QCM, valider les valeurs de nombreChoixMin et nombreChoixMax
            if (request.getNombreChoixMin() < 0) {
                throw new IllegalArgumentException("Le nombre minimum de choix doit être positif ou nul");
            }

            if (request.getNombreChoixMax() < request.getNombreChoixMin()) {
                throw new IllegalArgumentException(
                        String.format("Le nombre maximum de choix (%d) doit être >= au nombre minimum (%d)",
                                request.getNombreChoixMax(), request.getNombreChoixMin())
                );
            }

            // Vérifier que nombreChoixMax ne dépasse pas le nombre total d'options
            if (request.getNombreChoixMax() > options.size()) {
                throw new IllegalArgumentException(
                        String.format("Le nombre maximum de choix (%d) est supérieur au nombre de choix disponibles (%d)",
                                request.getNombreChoixMax(), options.size())
                );
            }

            // Pour la politique MOYENNE_BONNES_ET_MAUVAISES, vérifier qu'il y a au moins 2 bonnes réponses
            if (request.getPolitiqueCorrectionQCM() == PolitiqueCorrectionQCM.MOYENNE_BONNES_ET_MAUVAISES && nbOptionsCorrectes < 2) {
                throw new IllegalArgumentException(
                        "Pour la politique 'moyenne avec annulation', il faut au moins 2 réponses correctes"
                );
            }
        }

        // Ajouter la question avec les options
        QuestionAChoix question = examen.ajouterQuestionAChoix(
                request.getEnonce(),
                request.getBareme(),
                request.getTypeChoix(),
                request.getNombreChoixMin(),
                request.getNombreChoixMax(),
                request.getPolitiqueCorrectionQCM(),
                options
        );

        return examenRepository.save(examen);
    }

    public Examen ajouterQuestionDeveloppement(QuestionADeveloppementDTO request, Examen examen) {
        examen.ajouterQuestionADeveloppement(request.getEnonce(), request.getBareme());
        return examenRepository.save(examen);
    }

    public boolean supprimerQuestion(UUID questionId, Examen examen) {
        Question question = examen.getQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElse(null);

        if(question != null){
            examen.retirerQuestion(questionId);
            questionRepository.delete(question);
            examenRepository.save(examen);
            return true;
        }

        return false;
    }

}