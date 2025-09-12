package com.pooju.quizlet.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "Quiz")
public class Quiz {
    @Id
    @JsonPropertyDescription("format:5 digit random alphanumeric character")
    private String id;
    private String topic;
    private int difficulty;
    private List<Questions> questions;


}
