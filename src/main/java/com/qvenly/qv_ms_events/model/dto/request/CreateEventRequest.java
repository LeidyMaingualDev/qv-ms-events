package com.qvenly.qv_ms_events.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateEventRequest {

    @NotBlank(message = "El nombre del evento es obligatorio")
    private String title;

    private String description;
    private String location;

    @NotBlank(message = "El tipo de evento es obligatorio")
    private String eventType;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime startDatetime;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime endDatetime;
}
