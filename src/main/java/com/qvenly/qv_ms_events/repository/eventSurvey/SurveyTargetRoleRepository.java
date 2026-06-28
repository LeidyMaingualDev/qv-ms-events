package com.qvenly.qv_ms_events.repository.eventSurvey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qvenly.qv_ms_events.model.entity.eventSurvey.SurveyTargetRole;
import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyEventRole;

public interface SurveyTargetRoleRepository extends JpaRepository<SurveyTargetRole, Long>{

    List<SurveyTargetRole> findBySurveyId(Long surveyId);
 
    boolean existsBySurveyIdAndEventRole(Long surveyId, SurveyEventRole eventRole);
}
