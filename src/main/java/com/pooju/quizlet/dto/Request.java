package com.pooju.quizlet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Request {

    private String topic;
    private int difficulty;
    private Boolean refresh = Boolean.FALSE;
    private String sessionId;

}
