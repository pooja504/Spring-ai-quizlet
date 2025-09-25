package com.pooju.quizlet.service;

import com.pooju.quizlet.model.Quiz;
import com.pooju.quizlet.model.QuizRecommendation;
import com.pooju.quizlet.repository.QuizRecommendationRepo;
import com.pooju.quizlet.repository.QuizRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final ChatClient chatClient;
    private final QuizRepository quizRepository;
    private final EmbeddingService embeddingService;
    private final QuizRecommendationRepo quizRecommendationRepo;
    @Value("classpath:prompts/UserMessage.st")
    private Resource userMessage;
    @Value("classpath:prompts/SystemMessage.st")
    private Resource systemMessage;

    private Map<String, Set<String>> servedIds;
    private final Logger logger = Logger.getLogger(QuizService.class.getName());

    public QuizService(ChatClient chatClient, QuizRepository quizRepository, EmbeddingService embeddingService, QuizRecommendationRepo quizRecommendationRepo) {
        this.chatClient = chatClient;
        this.quizRepository = quizRepository;
        this.embeddingService = embeddingService;
        this.quizRecommendationRepo = quizRecommendationRepo;
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
        logger.info("SessionID: " + sessionID + ", Served IDs: " + servedIds.get(sessionID).size());

        Quiz quiz;

        if (!refresh) {
            List<Quiz> unservedQuiz = new ArrayList<>(getQuizFromDB(topic, difficulty)
                    .stream()
                    .filter(q -> !servedIds.get(sessionID).contains(q.getId()))
                    .toList());
            logger.info("Unserved quizzes available: " + unservedQuiz.size());

            if (!unservedQuiz.isEmpty()) {
                int randomIndex = ThreadLocalRandom.current().nextInt(unservedQuiz.size());
                quiz = unservedQuiz.get(randomIndex);
            } else {
                logger.info("No unserved quizzes available. Generating new quiz.");
                quiz = generateQuiz(topic, difficulty);
                quiz.setEmbedding(embeddingService.getEmbedding(quiz.getTopic()));
                quizRepository.save(quiz);
            }
        } else {
            logger.info("Refresh requested. Generating new quiz.");
            quiz = generateQuiz(topic, difficulty);
            quiz.setEmbedding(embeddingService.getEmbedding(quiz.getTopic()));
            quizRepository.save(quiz);
        }

        servedIds.get(sessionID).add(quiz.getId());
        return quiz;

    }

    public Quiz getQuizById(String id) {
        return quizRepository.findById(id)
                .orElse(null);
    }

    public void embedAllQuiz(){
        List<Quiz> quizzes=quizRepository.findAll();
        for (Quiz quiz:quizzes){
            if (quiz.getEmbedding()==null || quiz.getEmbedding().isEmpty()){
                quiz.setEmbedding(embeddingService.getEmbedding(quiz.getTopic()));
                logger.info("embedded quiz id "+quiz.getId());
                quizRepository.save(quiz);
            }
        }
    }

    public List<QuizRecommendation> getSimilarQuizzes(String topic){
        List<Double> queryVector=embeddingService.getEmbedding(topic);
        List<Quiz> similarQuizzes = quizRecommendationRepo.findSimilarQuizzes(queryVector, topic, 0.8);
        return mapToRecommendations(similarQuizzes);
    }

    public List<QuizRecommendation> mapToRecommendations(List<Quiz> quizzes) {
        return quizzes.stream()
                .collect(Collectors.groupingBy(
                        Quiz::getTopic,
                        Collectors.mapping(
                                Quiz::getId,
                                Collectors.toList()
                        )
                ))
                .entrySet().stream()
                .map(entry -> new QuizRecommendation(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

}
