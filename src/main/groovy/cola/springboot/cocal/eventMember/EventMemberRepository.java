package cola.springboot.cocal.eventMember;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventMemberRepository extends JpaRepository<EventMember, EventMemberId> {
    boolean existsById_EventIdAndId_UserId(Long eventId, Long userId);

    void deleteById_EventIdAndId_UserId(Long eventId, Long userId);
}
