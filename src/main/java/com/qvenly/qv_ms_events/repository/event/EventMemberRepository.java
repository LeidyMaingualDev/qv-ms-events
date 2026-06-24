package com.qvenly.qv_ms_events.repository.event;

import com.qvenly.qv_ms_events.model.entity.event.EventMember;
import com.qvenly.qv_ms_events.model.enums.event.EventRole;
import com.qvenly.qv_ms_events.model.enums.event.MemberStatus;
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

    /**
     *  Usuarios por evento diferenciados por rol
     * 
     * @return
     */
    @Query("""
    SELECT m.eventId,
        COUNT(CASE WHEN m.eventRole = 'STAFF'  THEN 1 END),
        COUNT(CASE WHEN m.eventRole = 'MEMBER' THEN 1 END)
    FROM EventMember m
    WHERE m.status = 'ACTIVE'
    GROUP BY m.eventId
    """)
    List<Object[]> countMembersByEventAndRole();


    /**
     * Total de usuarios diferenciados por rol
     */
    @Query("""
    SELECT 
        COUNT(CASE WHEN m.eventRole = 'STAFF'  THEN 1 END),
        COUNT(CASE WHEN m.eventRole = 'MEMBER' THEN 1 END)
    FROM EventMember m
    WHERE m.status = 'ACTIVE'
    """)
    List<Object[]> countGlobalByRole();
}
