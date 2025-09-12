package com.pooju.quizlet.controller;

import com.pooju.quizlet.model.Quiz;
import com.pooju.quizlet.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/Quiz/")
    public ResponseEntity<Quiz> getQuiz(@RequestParam("topic") String topic,
                                        @RequestParam("difficulty") int difficulty,
                                        @RequestParam(value = "refresh", defaultValue = "false") Boolean refresh,
                                        @RequestParam(value = "sessionID", required = true) String sessionID) {
        return ResponseEntity.ok(quizService.getQuiz(topic, difficulty, sessionID, refresh));

    }

    @GetMapping("/shared")
    public ResponseEntity<Quiz> sharedQuiz(@RequestParam(name = "quizId") String id) {
        Quiz quiz = quizService.getQuizById(id);
        if (quiz == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(quiz);
        }

    }

}
