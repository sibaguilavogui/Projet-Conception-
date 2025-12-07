package ca.uqac.examgu.model;

import jakarta.persistence.*;

@Entity
@Table(name = "questions_developpement")
@PrimaryKeyJoinColumn(name = "question_id")
public class QuestionADeveloppement extends Question {

    public QuestionADeveloppement() {
        super();
    }

    public QuestionADeveloppement(String enonce, double bareme, Examen examen) {
        super(enonce, bareme, examen);
    }

    @Override
    public String getType() {
        return "DEVELOPPEMENT";
    }

    @Override
    public String toString() {
        return String.format("QuestionADeveloppement{id=%s, enonce='%s', bareme=%.1f}",
                getId(), getEnonce(), getBareme());
    }
}