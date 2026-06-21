package com.qvenly.qv_ms_events.service.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.qvenly.qv_ms_events.model.dto.response.dashboardAdmin.EventByOrganizerResponseDTO;
import com.qvenly.qv_ms_events.model.dto.response.dashboardAdmin.EventUserDetailResponseDTO;
import com.qvenly.qv_ms_events.model.dto.response.dashboardAdmin.GlobalRoleStatsDTO;
import com.qvenly.qv_ms_events.repository.event.EventMemberRepository;
import com.qvenly.qv_ms_events.repository.event.EventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventStatsService {
    
    private final EventRepository eventRepository;
    private final EventMemberRepository memberRepository;

    // RF18 — total de eventos
    public Long getTotalEvents() {
        return eventRepository.count();
    }

    // RF20 — eventos por organizador
    public List<EventByOrganizerResponseDTO> getEventsByOrganizer(
            String startDate, String endDate) {

        LocalDateTime start = startDate != null
            ? LocalDate.parse(startDate).atStartOfDay() : null;
        LocalDateTime end = endDate != null
            ? LocalDate.parse(endDate).atTime(23, 59, 59) : null;

        return eventRepository.countEventsByOrganizer(start, end)
            .stream()
            .map(row -> new EventByOrganizerResponseDTO(
                (Long)   row[0],
                (String) row[1],
                (Long)   row[2]
            ))
            .toList();
    }

    // RF20.2 — usuarios por evento por rol
    public List<EventUserDetailResponseDTO> getUsersByEvent() {
    return memberRepository.countMembersByEventAndRole()
        .stream()
        .map(row -> {
            Long eventId = (Long) row[0];
            // busca el nombre del evento
            String eventName = eventRepository.findById(eventId)
                .map(e -> e.getTitle())
                .orElse("Evento " + eventId);

            return new EventUserDetailResponseDTO(
                eventId,
                eventName,
                (Long) row[1],
                (Long) row[2],
                (Long) row[3],
                (Long) row[4]
            );
        })
        .toList();
    }


    public GlobalRoleStatsDTO getGlobalRoleStats() {
        List<Object[]> rows = memberRepository.countGlobalByRole();
        Object[] result = rows.get(0);  // ← primer (y único) row
        Long organizers = eventRepository.countUniqueOrganizers();
        
        return new GlobalRoleStatsDTO(
            organizers,
            ((Number) result[0]).longValue(),
            ((Number) result[1]).longValue(),
            ((Number) result[2]).longValue(),
            ((Number) result[3]).longValue()
        );
    }

}