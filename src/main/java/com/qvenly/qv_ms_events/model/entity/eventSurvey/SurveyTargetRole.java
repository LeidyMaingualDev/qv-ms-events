package com.qvenly.qv_ms_events.model.entity.eventSurvey;

import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyEventRole;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter
@Entity
@Table(
    name = "survey_target_role",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_survey_role",
        columnNames = {"survey_id", "event_role"}
    )
)
public class SurveyTargetRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "event_role", nullable = false, length = 20)
    private SurveyEventRole eventRole;
}
