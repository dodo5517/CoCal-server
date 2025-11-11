package cola.springboot.cocal.eventMember;

import cola.springboot.cocal.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventMemberRepository extends JpaRepository<EventMember, EventMemberId> {
    boolean existsById_EventIdAndId_UserId(Long eventId, Long userId);
    void deleteByEventId(Long eventId);
    void deleteById_EventIdAndId_UserId(Long eventId, Long userId);

    @Query("""
        select  em.user
        from EventMember  em
        join em.event e
        where e.id = :eventId
    """)
    List<User> findUsersByEventId(@Param("eventId") Long eventId);

    /**
     * 여러 이벤트 ID에 속한 모든 멤버 + 유저를 한 번에 조회
     * - fetch join으로 User를 한 번에 로딩
     * - PostgreSQL IN 절 최적화: (event_id = ANY(:eventIds))
     */
    @Query("""
        select em
        from EventMember em
        join fetch em.user u
        where em.event.id in :eventIds
        """)
    List<EventMember> findAllByEventIds(@Param("eventIds") List<Long> eventIds);
}
