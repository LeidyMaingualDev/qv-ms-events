package com.qvenly.qv_ms_events.model.dto.response.dashboardAdmin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GlobalRoleStatsDTO {
    private Long totalOrganizers;
    private Long totalStaff;
    private Long totalAssistants;
    private Long totalJudges;
    private Long totalParticipants;
}
