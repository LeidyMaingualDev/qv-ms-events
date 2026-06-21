package com.qvenly.qv_ms_events.controller.event;

import com.qvenly.qv_ms_events.model.dto.response.dashboardAdmin.EventByOrganizerResponseDTO;
import com.qvenly.qv_ms_events.model.dto.response.dashboardAdmin.EventUserDetailResponseDTO;
import com.qvenly.qv_ms_events.model.dto.response.dashboardAdmin.GlobalRoleStatsDTO;
import com.qvenly.qv_ms_events.service.event.EventStatsService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping ("/api/events")
@RequiredArgsConstructor
public class EventStatsController {
      private final EventStatsService eventStatsService;

    @GetMapping("/total")
    public ResponseEntity<Long> getTotalEvents() {
        return ResponseEntity.ok(eventStatsService.getTotalEvents());
    }

    @GetMapping("/by-organizer")
    public ResponseEntity<List<EventByOrganizerResponseDTO>> getEventsByOrganizer(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate
    ) {
        return ResponseEntity.ok(
            eventStatsService.getEventsByOrganizer(startDate, endDate));
    }

    @GetMapping("/users-by-role")
    public ResponseEntity<List<EventUserDetailResponseDTO>> getUsersByEvent() {
        return ResponseEntity.ok(eventStatsService.getUsersByEvent());
    }

    @GetMapping("/global-roles")
    public ResponseEntity<GlobalRoleStatsDTO> getGlobalRoleStats() {
        return ResponseEntity.ok(eventStatsService.getGlobalRoleStats());
    }
}
