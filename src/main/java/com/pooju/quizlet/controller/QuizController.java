package com.pooju.quizlet.controller;

import com.pooju.quizlet.dto.Request;
import com.pooju.quizlet.model.Quiz;
import com.pooju.quizlet.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class QuizController {


    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/quiz")
    public ResponseEntity<Quiz> getQuiz(@RequestBody Request request) {
        return ResponseEntity.ok(quizService.getQuiz(request.getTopic(),
                request.getDifficulty(),
                request.getSessionId(),
                request.getRefresh()));

    }
}
