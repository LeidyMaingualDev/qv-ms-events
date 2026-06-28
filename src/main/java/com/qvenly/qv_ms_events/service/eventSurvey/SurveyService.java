package com.qvenly.qv_ms_events.service.eventSurvey;

import com.qvenly.qv_ms_events.client.event.ActivitiesClient;
import com.qvenly.qv_ms_events.client.event.NotificationClient;
import com.qvenly.qv_ms_events.exception.BusinessException;
import com.qvenly.qv_ms_events.model.dto.request.eventSurvey.SurveyCancelRequestDTO;
import com.qvenly.qv_ms_events.model.dto.request.eventSurvey.SurveyRequestDTO;
import com.qvenly.qv_ms_events.model.dto.request.eventSurvey.SurveyResponseRequestDTO;
import com.qvenly.qv_ms_events.model.dto.response.eventSurvey.SurveyResponseDTO;
import com.qvenly.qv_ms_events.model.dto.response.eventSurvey.SurveyResultsResponseDTO;
import com.qvenly.qv_ms_events.model.dto.response.eventSurvey.SurveySummaryResponseDTO;
import com.qvenly.qv_ms_events.model.entity.event.Event;
import com.qvenly.qv_ms_events.model.entity.eventSurvey.Survey;
import com.qvenly.qv_ms_events.model.entity.eventSurvey.SurveyAnswer;
import com.qvenly.qv_ms_events.model.entity.eventSurvey.SurveyOption;
import com.qvenly.qv_ms_events.model.entity.eventSurvey.SurveyQuestion;
import com.qvenly.qv_ms_events.model.entity.eventSurvey.SurveyResponse;
import com.qvenly.qv_ms_events.model.entity.eventSurvey.SurveyTargetRole;
import com.qvenly.qv_ms_events.model.enums.event.EventRole;
import com.qvenly.qv_ms_events.model.enums.event.EventStatus;
import com.qvenly.qv_ms_events.model.enums.eventSurvey.QuestionType;
import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyEventRole;
import com.qvenly.qv_ms_events.model.enums.eventSurvey.SurveyStatus;
import com.qvenly.qv_ms_events.repository.event.EventMemberRepository;
import com.qvenly.qv_ms_events.repository.eventSurvey.SurveyAnswerRepository;
import com.qvenly.qv_ms_events.repository.eventSurvey.SurveyRepository;
import com.qvenly.qv_ms_events.repository.eventSurvey.SurveyResponseRepository;
import com.qvenly.qv_ms_events.service.event.EventService;
import com.qvenly.qv_ms_events.client.event.AuthInternalClient;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository         surveyRepository;
    private final SurveyResponseRepository responseRepository;
    private final SurveyAnswerRepository   answerRepository;
    private final EventService             eventService;
    private final NotificationClient        notificationClient;
    private final EventMemberRepository     memberRepository;
    private final ActivitiesClient activitiesClient;
    private final AuthInternalClient authInternalClient;

    // ==================== RF130: Crear encuesta ====================

    @Transactional
    public SurveyResponseDTO createSurvey(Long eventId, SurveyRequestDTO req, Long performerUserId, String performerEmail, String systemRole) {
        Event event = eventService.findEventById(eventId);
        eventService.assertIsOrganizer(eventId, performerEmail, systemRole);

        if (req.getTargetRoles() == null || req.getTargetRoles().isEmpty()) {
            throw new BusinessException("Debe seleccionar al menos un grupo de destinatarios.", HttpStatus.BAD_REQUEST);
        }
        if (req.getQuestions() == null || req.getQuestions().isEmpty()) {
            throw new BusinessException("La encuesta debe tener al menos una pregunta.", HttpStatus.BAD_REQUEST);
        }

        Survey survey = new Survey();
        survey.setEvent(event);
        survey.setTitle(req.getTitle());
        survey.setDescription(req.getDescription());
        survey.setStatus(SurveyStatus.DRAFT);
        survey.setAnonymous(req.getAnonymous() != null && req.getAnonymous());
        survey.setDeadline(req.getDeadline());
        survey.setCreatedByUserId(performerUserId);
        survey.setCreatedByEmail(performerEmail);

        buildTargetRoles(survey, req.getTargetRoles());
        buildQuestions(survey, req.getQuestions());

        Survey saved = surveyRepository.save(survey);
        log.info("Encuesta creada: id={}, eventId={}", saved.getId(), eventId);
        return toResponse(saved);
    }

    // ==================== RF131: Publicar encuesta ====================

    @Transactional
public SurveyResponseDTO publishSurvey(Long surveyId, String performerEmail, String systemRole) {
    Survey survey = findSurveyById(surveyId);
    eventService.assertIsOrganizer(survey.getEvent().getId(), performerEmail, systemRole);

    if (survey.getStatus() != SurveyStatus.DRAFT) {
        throw new BusinessException("Solo se puede publicar una encuesta en estado DRAFT.", HttpStatus.CONFLICT);
    }
    if (survey.getEvent().getStatus() != EventStatus.IN_PROGRESS) {
        throw new BusinessException("Solo se puede publicar la encuesta cuando el evento haya iniciado.", HttpStatus.CONFLICT);
    }

    survey.setStatus(SurveyStatus.PUBLISHED);
    Survey updated = surveyRepository.save(survey);

    // Disparar notificaciones a los miembros con roles destinatarios
   try {
    // Roles destinatarios de la encuesta
    List<String> targetRoleNames = survey.getTargetRoles().stream()
            .map(tr -> tr.getEventRole().name())
            .toList();

    // STAFF viene de event_member, el resto de activity_member
    List<String> specificRoles = targetRoleNames.stream()
            .filter(r -> !r.equals("STAFF"))
            .toList();
    List<String> staffRoles = targetRoleNames.stream()
            .filter(r -> r.equals("STAFF"))
            .toList();

    List<NotificationClient.Recipient> recipients = new java.util.ArrayList<>();

    // Miembros con rol específico desde qv-ms-activities
    if (!specificRoles.isEmpty()) {
        activitiesClient.getMemberEmailsByRoles(survey.getEvent().getId(), specificRoles)
        .forEach(email -> {
            Map<String, Object> userData = authInternalClient.findUserByEmail(email);
            Long userId = userData != null && userData.get("userId") != null
                    ? ((Number) userData.get("userId")).longValue() : null;
            String name = userData != null ? (String) userData.get("name") : null;
            recipients.add(new NotificationClient.Recipient(userId, email, name));
        });
    }

    // STAFF desde event_member
    if (!staffRoles.isEmpty()) {
        memberRepository.findActiveByEventIdAndRoles(
                survey.getEvent().getId(),
                List.of(EventRole.STAFF))
                .forEach(m -> recipients.add(
                        new NotificationClient.Recipient(m.getUserId(), m.getUserEmail(), null)));
    }

    if (!recipients.isEmpty()) {
        String deadline = survey.getDeadline() != null
                ? survey.getDeadline().toString() : null;
        notificationClient.sendSurveyPublishedNotification(
                survey.getEvent().getTitle(),
                survey.getEvent().getId(),
                updated.getId(),
                survey.getTitle(),
                deadline,
                recipients);
    }
    } catch (Exception e) {
        log.warn("No se pudieron enviar notificaciones de encuesta: {}", e.getMessage());
    }

    log.info("Encuesta publicada: id={}", surveyId);
    return toResponse(updated);
}


    // ==================== RF132: Cancelar encuesta ====================

@Transactional
public SurveyResponseDTO cancelSurvey(Long surveyId, SurveyCancelRequestDTO req, String performerEmail, String systemRole) {
    Survey survey = findSurveyById(surveyId);
    eventService.assertIsOrganizer(survey.getEvent().getId(), performerEmail, systemRole);

    if (survey.getStatus() != SurveyStatus.PUBLISHED) {
        throw new BusinessException("Solo se puede cancelar una encuesta publicada.", HttpStatus.CONFLICT);
    }
    if (req.getCancelReason() == null || req.getCancelReason().isBlank()) {
        throw new BusinessException("Debe indicar el motivo de la cancelación.", HttpStatus.BAD_REQUEST);
    }

    survey.setStatus(SurveyStatus.CANCELLED);
    survey.setCancelReason(req.getCancelReason());
    Survey updated = surveyRepository.save(survey);

    // Disparar notificaciones a los mismos destinatarios de la encuesta
    try {
        List<String> targetRoleNames = survey.getTargetRoles().stream()
                .map(tr -> tr.getEventRole().name())
                .toList();

        List<String> specificRoles = targetRoleNames.stream()
                .filter(r -> !r.equals("STAFF"))
                .toList();
        List<String> staffRoles = targetRoleNames.stream()
                .filter(r -> r.equals("STAFF"))
                .toList();

        List<NotificationClient.Recipient> recipients = new java.util.ArrayList<>();

        if (!specificRoles.isEmpty()) {
            activitiesClient.getMemberEmailsByRoles(survey.getEvent().getId(), specificRoles)
                    .forEach(email -> {
                        Map<String, Object> userData = authInternalClient.findUserByEmail(email);
                        Long userId = userData != null && userData.get("userId") != null
                                ? ((Number) userData.get("userId")).longValue() : null;
                        String name = userData != null ? (String) userData.get("name") : null;
                        recipients.add(new NotificationClient.Recipient(userId, email, name));
                    });
        }

        if (!staffRoles.isEmpty()) {
            memberRepository.findActiveByEventIdAndRoles(
                            survey.getEvent().getId(),
                            List.of(EventRole.STAFF))
                    .forEach(m -> recipients.add(
                            new NotificationClient.Recipient(m.getUserId(), m.getUserEmail(), null)));
        }

        if (!recipients.isEmpty()) {
            notificationClient.sendSurveyCancelledNotification(
                    survey.getEvent().getTitle(),
                    survey.getEvent().getId(),
                    updated.getId(),
                    survey.getTitle(),
                    req.getCancelReason(),
                    recipients);
        }
    } catch (Exception e) {
        log.warn("No se pudieron enviar notificaciones de cancelación de encuesta: {}", e.getMessage());
    }

    log.info("Encuesta cancelada: id={}, motivo={}", surveyId, req.getCancelReason());
    return toResponse(updated);
}

    // ==================== RF133/RF134/RF135: Responder encuesta ====================

    @Transactional
    public void submitResponse(Long surveyId, SurveyResponseRequestDTO req, Long userId, String userEmail) {
        Survey survey = findSurveyById(surveyId);

        if (survey.getStatus() != SurveyStatus.PUBLISHED) {
            throw new BusinessException("La encuesta no está disponible para responder.", HttpStatus.CONFLICT);
        }
        if (survey.getEvent().getStatus() != EventStatus.IN_PROGRESS) {
            throw new BusinessException("Solo se puede responder mientras el evento esté activo.", HttpStatus.CONFLICT);
        }
        if (survey.getDeadline() != null && LocalDateTime.now().isAfter(survey.getDeadline())) {
            throw new BusinessException("La fecha límite para responder esta encuesta ya venció.", HttpStatus.CONFLICT);
        }
        boolean roleAllowed = survey.getTargetRoles().stream()
                .anyMatch(tr -> tr.getEventRole() == req.getRespondentRole());
        if (!roleAllowed) {
            throw new BusinessException("Tu rol no está habilitado para responder esta encuesta.", HttpStatus.FORBIDDEN);
        }
        if (!survey.getAnonymous() && responseRepository.existsBySurveyIdAndUserId(surveyId, userId)) {
            throw new BusinessException("Ya respondiste esta encuesta.", HttpStatus.CONFLICT);
        }

        SurveyResponse response = new SurveyResponse();
        response.setSurvey(survey);
        response.setRespondentRole(req.getRespondentRole());

        if (!survey.getAnonymous()) {
            response.setUserId(userId);
            response.setUserEmail(userEmail);
        }

        Map<Long, SurveyQuestion> questionsById = survey.getQuestions().stream()
                .collect(Collectors.toMap(SurveyQuestion::getId, q -> q));

        for (SurveyResponseRequestDTO.AnswerRequest ansReq : req.getAnswers()) {
            SurveyQuestion question = questionsById.get(ansReq.getQuestionId());
            if (question == null) {
                throw new BusinessException("La pregunta " + ansReq.getQuestionId() + " no pertenece a esta encuesta.", HttpStatus.BAD_REQUEST);
            }
            if (question.getRequired()
                    && (ansReq.getOpenAnswer() == null || ansReq.getOpenAnswer().isBlank())
                    && ansReq.getSelectedOptionId() == null) {
                throw new BusinessException("La pregunta '" + question.getQuestionText() + "' es obligatoria.", HttpStatus.BAD_REQUEST);
            }

            SurveyAnswer answer = new SurveyAnswer();
            answer.setResponse(response);
            answer.setQuestion(question);

            if (question.getQuestionType() == QuestionType.OPEN_TEXT) {
                answer.setOpenAnswer(ansReq.getOpenAnswer());
            } else if (question.getQuestionType() == QuestionType.RATING) {
                if (ansReq.getOpenAnswer() == null || ansReq.getOpenAnswer().isBlank()) {
                    throw new BusinessException("Debes ingresar una calificación.", HttpStatus.BAD_REQUEST);
                }
                answer.setOpenAnswer(ansReq.getOpenAnswer());
            } else if (ansReq.getSelectedOptionId() != null) {
                SurveyOption option = question.getOptions().stream()
                        .filter(o -> o.getId().equals(ansReq.getSelectedOptionId()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("La opción seleccionada no pertenece a la pregunta.", HttpStatus.BAD_REQUEST));
                answer.setSelectedOption(option);
            }

            response.getAnswers().add(answer);
        }

        responseRepository.save(response);
        log.info("Respuesta registrada en encuesta id={}, anónima={}", surveyId, survey.getAnonymous());
    }

    // ==================== RF136/RF137: Ver resultados ====================

    public SurveyResultsResponseDTO getResults(Long surveyId, SurveyEventRole roleFilter, String performerEmail, String systemRole) {
        Survey survey = findSurveyById(surveyId);
        eventService.assertIsOrganizer(survey.getEvent().getId(), performerEmail, systemRole);

        List<SurveyResponse> responses = roleFilter != null
                ? responseRepository.findBySurveyIdAndRoleWithAnswers(surveyId, roleFilter)
                : responseRepository.findBySurveyIdWithAnswers(surveyId);

        SurveyResultsResponseDTO result = new SurveyResultsResponseDTO();
        result.setSurveyId(survey.getId());
        result.setSurveyTitle(survey.getTitle());
        result.setTotalResponses((long) responses.size());

        Map<SurveyEventRole, Long> byRole = new HashMap<>();
        for (SurveyEventRole role : SurveyEventRole.values()) {
            byRole.put(role, responseRepository.countBySurveyIdAndRespondentRole(surveyId, role));
        }
        result.setResponsesByRole(byRole);
        result.setQuestionResults(buildQuestionResults(survey, responses));
        result.setTargetRoles(survey.getTargetRoles().stream()
                .map(SurveyTargetRole::getEventRole)
                .toList());
        return result;
    }

    // ==================== Listados ====================

    public List<SurveySummaryResponseDTO> getSurveysByEvent(Long eventId, String performerEmail, String systemRole) {
        eventService.assertIsOrganizer(eventId, performerEmail, systemRole);
        return surveyRepository.findByEventIdOrderByCreatedAtDesc(eventId).stream()
                .map(this::toSummary)
                .toList();
    }

    public SurveyResponseDTO getSurveyDetail(Long surveyId) {
        Survey survey = surveyRepository.findByIdWithQuestionsAndRoles(surveyId)
                .orElseThrow(() -> new EntityNotFoundException("Encuesta no encontrada: " + surveyId));
        return toResponse(survey);
    }

    // ==================== Privados: construcción de entidades ====================

    private void buildTargetRoles(Survey survey, List<SurveyEventRole> roles) {
        for (SurveyEventRole role : roles) {
            SurveyTargetRole target = new SurveyTargetRole();
            target.setSurvey(survey);
            target.setEventRole(role);
            survey.getTargetRoles().add(target);
        }
    }

    private void buildQuestions(Survey survey, List<SurveyRequestDTO.QuestionRequest> questions) {
        int order = 0;
        for (SurveyRequestDTO.QuestionRequest qReq : questions) {
            SurveyQuestion question = new SurveyQuestion();
            question.setSurvey(survey);
            question.setQuestionText(qReq.getQuestionText());
            question.setQuestionType(qReq.getQuestionType());
            question.setDisplayOrder(qReq.getDisplayOrder() != null ? qReq.getDisplayOrder() : order++);
            question.setRequired(qReq.getRequired() == null || qReq.getRequired());

            boolean isChoiceType = qReq.getQuestionType() == QuestionType.SINGLE_CHOICE
                    || qReq.getQuestionType() == QuestionType.MULTIPLE_CHOICE;
            if (isChoiceType) {
                if (qReq.getOptions() == null || qReq.getOptions().size() < 2) {
                    throw new BusinessException("Las preguntas de selección requieren al menos 2 opciones.", HttpStatus.BAD_REQUEST);
                }
                int optOrder = 0;
                for (SurveyRequestDTO.OptionRequest optReq : qReq.getOptions()) {
                    SurveyOption option = new SurveyOption();
                    option.setQuestion(question);
                    option.setOptionText(optReq.getOptionText());
                    option.setDisplayOrder(optReq.getDisplayOrder() != null ? optReq.getDisplayOrder() : optOrder++);
                    question.getOptions().add(option);
                }
            }
            survey.getQuestions().add(question);
        }
    }

    public List<SurveySummaryResponseDTO> getPendingSurveys(Long eventId, Long userId, String userEmail) {
    // Obtener roles del usuario en el evento desde activity_member
    List<String> activityRoles = activitiesClient.getMemberRolesByEmailAndEvent(userEmail, eventId);

    if (activityRoles.isEmpty()) return List.of();

    List<SurveyEventRole> surveyRoles = activityRoles.stream()
            .map(r -> {
                try { return SurveyEventRole.valueOf(r); }
                catch (IllegalArgumentException e) { return null; }
            })
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();

    if (surveyRoles.isEmpty()) return List.of();

    return surveyRepository.findPendingForUser(eventId, surveyRoles, userId)
            .stream()
            .map(this::toSummary)
            .toList();
}

    private List<SurveyResultsResponseDTO.QuestionResultResponse> buildQuestionResults(
            Survey survey, List<SurveyResponse> responses) {

        List<SurveyAnswer> allAnswers = responses.stream()
                .flatMap(r -> r.getAnswers().stream())
                .toList();

        return survey.getQuestions().stream()
                .sorted((a, b) -> a.getDisplayOrder().compareTo(b.getDisplayOrder()))
                .map(question -> {
                    List<SurveyAnswer> questionAnswers = allAnswers.stream()
                            .filter(a -> a.getQuestion().getId().equals(question.getId()))
                            .toList();

                    SurveyResultsResponseDTO.QuestionResultResponse qr = new SurveyResultsResponseDTO.QuestionResultResponse();
                    qr.setQuestionId(question.getId());
                    qr.setQuestionText(question.getQuestionText());
                    qr.setQuestionType(question.getQuestionType());
                    qr.setTotalAnswers((long) questionAnswers.size());

                    if (question.getQuestionType() == QuestionType.OPEN_TEXT) {
                        qr.setOpenAnswers(questionAnswers.stream()
                                .map(SurveyAnswer::getOpenAnswer)
                                .filter(text -> text != null && !text.isBlank())
                                .toList());

                    } else if (question.getQuestionType() == QuestionType.RATING) {
                        List<Integer> ratings = questionAnswers.stream()
                                .map(SurveyAnswer::getOpenAnswer)
                                .filter(v -> v != null && !v.isBlank())
                                .map(v -> {
                                    try { return Integer.parseInt(v.trim()); }
                                    catch (NumberFormatException e) { return null; }
                                })
                                .filter(v -> v != null && v >= 1 && v <= 5)
                                .toList();

                        double avg = ratings.isEmpty() ? 0.0 :
                                ratings.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                        qr.setAverageRating(avg);
                        qr.setResponseCount((long) ratings.size());

                        Map<Integer, Long> distribution = new LinkedHashMap<>();
                        for (int i = 1; i <= 5; i++) {
                            final int star = i;
                            distribution.put(star, ratings.stream().filter(r -> r == star).count());
                        }
                        qr.setRatingDistribution(distribution);

                    } else {
                        long totalSelections = questionAnswers.stream()
                                .filter(a -> a.getSelectedOption() != null)
                                .count();
                        qr.setOptionResults(question.getOptions().stream()
                                .map(option -> {
                                    long count = questionAnswers.stream()
                                            .filter(a -> a.getSelectedOption() != null
                                                    && a.getSelectedOption().getId().equals(option.getId()))
                                            .count();
                                    SurveyResultsResponseDTO.OptionResultResponse or = new SurveyResultsResponseDTO.OptionResultResponse();
                                    or.setOptionId(option.getId());
                                    or.setOptionText(option.getOptionText());
                                    or.setCount(count);
                                    or.setPercentage(totalSelections > 0 ? (count * 100.0 / totalSelections) : 0.0);
                                    return or;
                                })
                                .toList());
                    }

                    return qr;
                })
                .toList();
    }

    private Survey findSurveyById(Long surveyId) {
        return surveyRepository.findById(surveyId)
                .orElseThrow(() -> new EntityNotFoundException("Encuesta no encontrada: " + surveyId));
    }

    // ==================== Mapeo a DTOs ====================

    private SurveySummaryResponseDTO toSummary(Survey s) {
        SurveySummaryResponseDTO r = new SurveySummaryResponseDTO();
        r.setId(s.getId());
        r.setTitle(s.getTitle());
        r.setStatus(s.getStatus());
        r.setAnonymous(s.getAnonymous());
        r.setDeadline(s.getDeadline());
        r.setCreatedAt(s.getCreatedAt());
        r.setTotalResponses(responseRepository.countBySurveyId(s.getId()));
        r.setTargetRoles(s.getTargetRoles().stream()
                .map(SurveyTargetRole::getEventRole)
                .toList());
        r.setQuestionCount(s.getQuestions().size());
        return r;
    }

    private SurveyResponseDTO toResponse(Survey s) {
        SurveyResponseDTO r = new SurveyResponseDTO();
        r.setId(s.getId());
        r.setEventId(s.getEvent().getId());
        r.setTitle(s.getTitle());
        r.setDescription(s.getDescription());
        r.setStatus(s.getStatus());
        r.setAnonymous(s.getAnonymous());
        r.setDeadline(s.getDeadline());
        r.setCancelReason(s.getCancelReason());
        r.setCreatedByUserId(s.getCreatedByUserId());
        r.setCreatedByEmail(s.getCreatedByEmail());
        r.setCreatedAt(s.getCreatedAt());
        r.setUpdatedAt(s.getUpdatedAt());
        r.setTargetRoles(s.getTargetRoles().stream().map(SurveyTargetRole::getEventRole).toList());
        r.setQuestions(s.getQuestions().stream()
                .sorted((a, b) -> a.getDisplayOrder().compareTo(b.getDisplayOrder()))
                .map(this::toQuestionResponse)
                .toList());
        return r;
    }

    private SurveyResponseDTO.QuestionResponse toQuestionResponse(SurveyQuestion q) {
        SurveyResponseDTO.QuestionResponse qr = new SurveyResponseDTO.QuestionResponse();
        qr.setId(q.getId());
        qr.setQuestionText(q.getQuestionText());
        qr.setQuestionType(q.getQuestionType());
        qr.setDisplayOrder(q.getDisplayOrder());
        qr.setRequired(q.getRequired());
        qr.setOptions(q.getOptions().stream()
                .sorted((a, b) -> a.getDisplayOrder().compareTo(b.getDisplayOrder()))
                .map(this::toOptionResponse)
                .toList());
        return qr;
    }

    private SurveyResponseDTO.OptionResponse toOptionResponse(SurveyOption o) {
        SurveyResponseDTO.OptionResponse or = new SurveyResponseDTO.OptionResponse();
        or.setId(o.getId());
        or.setOptionText(o.getOptionText());
        or.setDisplayOrder(o.getDisplayOrder());
        return or;
    }
}