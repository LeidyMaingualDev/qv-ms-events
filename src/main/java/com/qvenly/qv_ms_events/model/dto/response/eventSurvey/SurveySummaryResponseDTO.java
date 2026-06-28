package com.qvenly.qv_ms_events.model.dto.response.eventSurvey;

import java.time.LocalDateTime;

import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyEventRole;
import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyStatus;

import lombok.Data;
import java.util.List;

@Data
public class SurveySummaryResponseDTO {
    private Long id;
    private String title;
    private SurveyStatus status;
    private Boolean anonymous;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;
    private Long totalResponses;
     private List<SurveyEventRole> targetRoles; 
    private Integer questionCount;  
}
