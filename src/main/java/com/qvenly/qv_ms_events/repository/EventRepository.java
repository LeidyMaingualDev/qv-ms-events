package com.qvenly.qv_ms_events.repository;

import com.qvenly.qv_ms_events.model.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
