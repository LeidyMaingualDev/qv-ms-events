package com.qvenly.qv_ms_events.repository.event;

import com.qvenly.qv_ms_events.model.entity.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.ownerUserId = :ownerId " +
           "AND e.status <> 'CANCELLED'")
    long countActiveEventsByOwner(@Param("ownerId") Long ownerId);

    @Query("SELECT e FROM Event e WHERE e.id IN " +
           "(SELECT m.eventId FROM EventMember m " +
           " WHERE m.userEmail = :email AND m.status = 'ACTIVE')")
    List<Event> findEventsByMemberEmail(@Param("email") String email);


    /**
     * Eventos por organizador con filtro de fechas
     * 
     * @param startDate
     * @param endDate
     * @return eventos por organizador
     */
       @Query("""
              SELECT e.ownerUserId, e.ownerEmail, COUNT(e)
              FROM Event e
              WHERE e.status <> 'CANCELLED'
              AND (:startDate IS NULL OR e.startDatetime >= :startDate)
              AND (:endDate   IS NULL OR e.startDatetime <= :endDate)
              GROUP BY e.ownerUserId, e.ownerEmail
              """)

       List<Object[]> countEventsByOrganizer(
       @Param("startDate") LocalDateTime startDate,
       @Param("endDate")   LocalDateTime endDate
       );
}
