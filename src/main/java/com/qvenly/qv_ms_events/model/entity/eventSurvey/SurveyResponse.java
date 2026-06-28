package com.qvenly.qv_ms_events.model.entity.eventSurvey;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyEventRole;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "survey_response")
public class SurveyResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;
 
    // Nullable cuando la encuesta es anónima (RF134)
    @Column(name = "user_id")
    private Long userId;
 
    @Column(name = "user_email", length = 100)
    private String userEmail;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "respondent_role", nullable = false, length = 20)
    private SurveyEventRole respondentRole;
 
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;
 
    @OneToMany(mappedBy = "response", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SurveyAnswer> answers = new ArrayList<>();
 
    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }
}
