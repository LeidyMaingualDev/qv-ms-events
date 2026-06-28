package com.qvenly.qv_ms_events.model.dto.request.eventSurvey;

import java.time.LocalDateTime;
import java.util.List;

import com.qvenly.qv_ms_events.model.enums.eventSurvey.QuestionType;
import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyEventRole;

import lombok.Data;

@Data
public class SurveyRequestDTO {
    private String title;
    private String description;
    private Boolean anonymous;
    private LocalDateTime deadline;
    private List<SurveyEventRole> targetRoles;
    private List<QuestionRequest> questions;
 
    @Data
    public static class QuestionRequest {
        private String questionText;
        private QuestionType questionType;
        private Integer displayOrder;
        private Boolean required;
        private List<OptionRequest> options; // solo para SINGLE_CHOICE y MULTIPLE_CHOICE
    }
 
    @Data
    public static class OptionRequest {
        private String optionText;
        private Integer displayOrder;
    }
}
