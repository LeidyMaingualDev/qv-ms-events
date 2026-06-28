package com.qvenly.qv_ms_events.repository.eventSurvey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qvenly.qv_ms_events.model.entity.eventSurvey.SurveyOption;

public interface SurveyOptionRepository extends JpaRepository<SurveyOption, Long>{
     List<SurveyOptionRepository> findByQuestionIdOrderByDisplayOrderAsc(Long questionId);
}
