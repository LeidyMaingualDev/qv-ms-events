package com.qvenly.qv_ms_events.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;

import com.qvenly.qv_ms_events.model.entity.eventSurvey.Survey;
import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyStatus;
import com.qvenly.qv_ms_events.repository.eventSurvey.SurveyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SurveyExpirationScheduler {

    private final SurveyRepository surveyRepository;

    @Scheduled(cron = "0 */5 * * * *") // cada minuto
    @Transactional
    public void closeExpiredSurveys() {
        List<Survey> expired = surveyRepository.findExpiredSurveys(LocalDateTime.now());
        if (expired.isEmpty()) return;

        expired.forEach(s -> s.setStatus(SurveyStatus.CLOSED));
        surveyRepository.saveAll(expired);

        log.info("Encuestas expiradas cerradas automáticamente: {}", expired.size());
    }
}
