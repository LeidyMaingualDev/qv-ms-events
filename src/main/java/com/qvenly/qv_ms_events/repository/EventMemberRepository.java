package com.qvenly.qv_ms_events.repository;

import com.qvenly.qv_ms_events.model.entity.EventMember;
import com.qvenly.qv_ms_events.model.enums.EventRole;
import com.qvenly.qv_ms_events.model.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventMemberRepository extends JpaRepository<EventMember, Long> {

    List<EventMember> findByEventIdAndStatus(Long eventId, MemberStatus status);

    List<EventMember> findByEventIdAndEventRoleAndStatus(Long eventId, EventRole role, MemberStatus status);

    Optional<EventMember> findByEventIdAndUserEmailAndEventRole(Long eventId, String userEmail, EventRole role);

    boolean existsByEventIdAndUserEmailAndStatus(Long eventId, String userEmail, MemberStatus status);

    boolean existsByEventIdAndUserEmailAndEventRoleAndStatus(
            Long eventId, String userEmail, EventRole role, MemberStatus status);

    @Query("SELECT COUNT(m) FROM EventMember m WHERE m.eventId = :eventId " +
           "AND m.eventRole = :role AND m.status = 'ACTIVE'")
    long countActiveByEventAndRole(@Param("eventId") Long eventId, @Param("role") EventRole role);
}
