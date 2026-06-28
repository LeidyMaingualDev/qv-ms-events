package com.qvenly.qv_ms_events.model.dto.response.eventSurvey;

import java.util.List;
import java.util.Map;

import com.qvenly.qv_ms_events.model.enums.eventSurvey.QuestionType;
import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyEventRole;

import lombok.Data;

@Data
public class SurveyResultsResponseDTO {
    private Long surveyId;
    private String surveyTitle;
    private Long totalResponses;
    private Map<SurveyEventRole, Long> responsesByRole; // para RF137
    private List<QuestionResultResponse> questionResults;
    private List<SurveyEventRole> targetRoles;
 
    @Data
    public static class QuestionResultResponse {
        private Long questionId;
        private String questionText;
        private QuestionType questionType;
        private Long totalAnswers;
        private List<OptionResultResponse> optionResults; // para preguntas cerradas
        private List<String> openAnswers;                 // para preguntas abiertas (RF135)
        private Double averageRating;
        private Long responseCount;
        private Map<Integer, Long> ratingDistribution;
    }
 
    @Data
    public static class OptionResultResponse {
        private Long optionId;
        private String optionText;
        private Long count;
        private Double percentage;
    }
}
