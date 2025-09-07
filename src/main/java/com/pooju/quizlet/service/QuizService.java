package com.pooju.quizlet.service;

import com.pooju.quizlet.model.Quiz;
import com.pooju.quizlet.repository.QuizRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Service
public class QuizService {

    private final ChatClient chatClient;
    private final QuizRepository quizRepository;
    @Value("classpath:prompts/UserMessage.st")
    private Resource userMessage;
    @Value("classpath:prompts/SystemMessage.st")
    private Resource systemMessage;

    private Map<String, Set<String>> servedIds;
    private final Logger logger = Logger.getLogger(QuizService.class.getName());

    public QuizService(ChatClient chatClient, QuizRepository quizRepository) {
        this.chatClient = chatClient;
        this.quizRepository = quizRepository;
        servedIds = new ConcurrentHashMap<>();
    }

    private Quiz generateQuiz(String topic,int difficulty){

        SystemMessage systemMessage1= new SystemMessage(systemMessage);
        PromptTemplate promptTemplate= new PromptTemplate(userMessage);
        Prompt prompt=promptTemplate.create(Map.of(
                "topic", topic,
                "difficulty",difficulty
        ));
        UserMessage userMessage1=new UserMessage(prompt.getContents());
        return chatClient
                .prompt(new Prompt(
                        List.of(systemMessage1,userMessage1)
                ))
                .call()
                .entity(Quiz.class);
    }

    private List<Quiz> getQuizFromDB(String topic, int difficulty){
       return quizRepository
               .findByTopicAndDifficulty(topic,difficulty);

    }

    public Quiz getQuiz(String topic, int difficulty, String sessionID, Boolean refresh) {

        servedIds.putIfAbsent(sessionID, ConcurrentHashMap.newKeySet());
        logger.info("SessionID: " + sessionID + ", Served IDs: " + servedIds.get(sessionID).toString());

        List<Quiz> unservedQuiz = new ArrayList<>(getQuizFromDB(topic, difficulty)
                .stream()
                .filter(quiz -> !servedIds.get(sessionID).contains(quiz.getId()))
                .toList());
        logger.info("Unserved Quiz Count: " + unservedQuiz.size());
        if (!refresh){
            if (!unservedQuiz.isEmpty()) {
                Collections.shuffle(unservedQuiz);
                servedIds.get(sessionID).add(unservedQuiz.get(0).getId());
                return unservedQuiz.get(0);
            }else {
                logger.info("No unserved quizzes available. Generating new quiz.");
                Quiz quiz = generateQuiz(topic, difficulty);
                quizRepository.save(quiz);
                return quiz;
            }
        } else {
            logger.info("Refresh requested. Generating new quiz.");
            Quiz quiz = generateQuiz(topic, difficulty);
            quizRepository.save(quiz);
            servedIds.get(sessionID).add(quiz.getId());
            return quiz;

        }

    }
}
