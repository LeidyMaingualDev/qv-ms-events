package com.qvenly.qv_ms_events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio qv-ms-events.
 * Gestiona eventos, invitaciones y miembros — Módulos 6, 7 y 8.
 *
 * @author Equipo Qvenly
 */
@SpringBootApplication
public class QvMsEventsApplication {

    public static void main(String[] args) {
        SpringApplication.run(QvMsEventsApplication.class, args);
    }
}
