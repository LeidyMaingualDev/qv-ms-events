package com.qvenly.qv_ms_events.repository.eventSurvey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qvenly.qv_ms_events.model.entity.eventSurvey.SurveyQuestion;

public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {
      List<SurveyQuestion> findBySurveyIdOrderByDisplayOrderAsc(Long surveyId);
}
