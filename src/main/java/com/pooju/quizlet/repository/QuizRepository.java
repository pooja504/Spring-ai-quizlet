package com.pooju.quizlet.repository;

import com.pooju.quizlet.model.Quiz;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface QuizRepository extends MongoRepository<Quiz,String> {

    @Query("{'topic': {$regex: ?0, $options: 'i'}, 'difficulty': ?1}")
    public List<Quiz> findByTopicAndDifficulty(String topic, int difficulty);

}
