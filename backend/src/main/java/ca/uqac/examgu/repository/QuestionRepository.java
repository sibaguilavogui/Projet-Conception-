package ca.uqac.examgu.repository;

import ca.uqac.examgu.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Optional<Question> findById(UUID questionId);
}