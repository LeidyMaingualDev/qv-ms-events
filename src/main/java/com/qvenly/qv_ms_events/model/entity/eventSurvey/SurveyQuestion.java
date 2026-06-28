package com.qvenly.qv_ms_events.model.entity.eventSurvey;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.qvenly.qv_ms_events.model.enums.eventSurvey.QuestionType;

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
@Table(name = "survey_question")
public class SurveyQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;
 
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType;
 
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
 
    @Column(nullable = false)
    private Boolean required = true;
 
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
 
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    private List<SurveyOption> options = new ArrayList<>();
 
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
