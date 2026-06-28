package com.qvenly.qv_ms_events.model.entity.eventSurvey;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.qvenly.qv_ms_events.model.entity.event.Event;
import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyStatus;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

 
@Getter
@Setter
@Entity
@Table(name = "survey")
public class Survey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
 
    @Column(nullable = false, length = 150)
    private String title;
 
    @Column(columnDefinition = "TEXT")
    private String description;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SurveyStatus status = SurveyStatus.DRAFT;
 
    @Column(nullable = false)
    private Boolean anonymous = false;
 
    @Column(name = "deadline")
    private LocalDateTime deadline;
 
    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;
 
    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;
 
    @Column(name = "created_by_email", nullable = false, length = 100)
    private String createdByEmail;
 
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
 
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
 
    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    private List<SurveyQuestion> questions = new ArrayList<>();

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    private List<SurveyTargetRole> targetRoles = new ArrayList<>();
 
    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    private List<SurveyResponse> responses = new ArrayList<>();
 
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
 
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
