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
}
