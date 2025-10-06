package cola.springboot.cocal.eventMember;

import cola.springboot.cocal.event.Event;
import cola.springboot.cocal.event.EventRepository;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventMemberService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMemberRepository eventMemberRepository;

    @Transactional
    public void addMember(Long eventId, Long userId) {
        Event event = eventRepository.getReferenceById(eventId);
        User user = userRepository.getReferenceById(userId);

        if (eventMemberRepository.existsById_EventIdAndId_UserId(eventId, userId)) {
            return; // 이미 있으면 무시
        }
        eventMemberRepository.save(EventMember.of(event, user));
    }

    // 제거
    @Transactional
    public void removeMember(Long eventId, Long userId) {
        eventMemberRepository.deleteById_EventIdAndId_UserId(eventId, userId);
    }
}
