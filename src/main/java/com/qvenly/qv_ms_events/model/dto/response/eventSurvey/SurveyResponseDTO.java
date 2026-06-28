package com.qvenly.qv_ms_events.model.dto.response.eventSurvey;

import java.time.LocalDateTime;
import java.util.List;

import com.qvenly.qv_ms_events.model.enums.eventSurvey.QuestionType;
import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyEventRole;
import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyStatus;

import lombok.Data;

@Data
public class SurveyResponseDTO {
    
     private Long id;
    private Long eventId;
    private String title;
    private String description;
    private SurveyStatus status;
    private Boolean anonymous;
    private LocalDateTime deadline;
    private String cancelReason;
    private Long createdByUserId;
    private String createdByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SurveyEventRole> targetRoles;
    private List<QuestionResponse> questions;

    @Data
    public static class QuestionResponse {
        private Long id;
        private String questionText;
        private QuestionType questionType;
        private Integer displayOrder;
        private Boolean required;
        private List<OptionResponse> options;
    }

    @Data
    public static class OptionResponse {
        private Long id;
        private String optionText;
        private Integer displayOrder;
    }
}
