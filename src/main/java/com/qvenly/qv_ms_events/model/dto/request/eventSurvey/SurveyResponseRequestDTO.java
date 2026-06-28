package com.qvenly.qv_ms_events.model.dto.request.eventSurvey;

import java.util.List;

import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyEventRole;

import lombok.Data;

@Data
public class SurveyResponseRequestDTO {
    private SurveyEventRole respondentRole;
    private List<AnswerRequest> answers;
 
    @Data
    public static class AnswerRequest {
        private Long questionId;
        private Long selectedOptionId; // para preguntas cerradas
        private String openAnswer;     // para preguntas abiertas y campo libre (RF135)
    }
}
