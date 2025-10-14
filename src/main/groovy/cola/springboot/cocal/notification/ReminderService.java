package cola.springboot.cocal.notification;

import cola.springboot.cocal.event.Event;
import cola.springboot.cocal.event.EventRepository;
import cola.springboot.cocal.eventMember.EventMemberRepository;
import cola.springboot.cocal.todo.private_todo.PrivateTodo;
import cola.springboot.cocal.todo.private_todo.PrivateTodoRepository;
import cola.springboot.cocal.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderService {
    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final PrivateTodoRepository privateTodoRepository;

    // 1분마다 실행, event offsetMinutes 알림
    @Scheduled(fixedRate = 60000)
    public void sendEventReminders() {
        LocalDateTime now = LocalDateTime.now();
        /*System.out.println("Checking events at: " + now);*/

        // offset 기준으로 1분 이내 시작하는 이벤트 찾기
        // repository 쿼리에서 startAt - offsetMinutes 기준으로 1분 범위 내 이벤트 조회
        // 이벤트 알림
        List<Event> upcomingEvents = eventRepository.findEventsStartingAfterOffset(now);
        /*System.out.println("Found " + upcomingEvents.size() + " upcoming events.");*/

        for (Event event : upcomingEvents) {
            // offsetMinutes 기준 알림 시간 계산
            LocalDateTime reminderTime = event.getStartAt().minusMinutes(event.getOffsetMinutes());

            // 현재 시간이 reminderTime + 1분 범위를 벗어나면 skip
            if (now.isBefore(reminderTime) || now.isAfter(reminderTime.plusMinutes(1))) continue;

            // 이벤트에 등록된 팀원 전부 불러오기
            List<User> members = eventMemberRepository.findUsersByEventId(event.getId());

            for (User member : members) {
                // 이미 알림이 전송됐는지 확인 (중복 방지)
                boolean alreadySent = notificationRepository.existsByUserIdAndReferenceIdAndType(
                        member.getId(), event.getId(), "EVENT"
                );

                if (alreadySent) {
                    // 이미 알림 보냈으면 skip
                    continue;
                }

                // 팀원에게 알림 전송
                String message = event.getOffsetMinutes() == 0
                        ? event.getTitle() + " 이벤트가 지금 시작합니다."
                        : event.getTitle() + " 이벤트가 " + event.getOffsetMinutes() + "분 후 시작합니다.";

                notificationService.sendNotification(
                        member.getId(),
                        "EVENT",
                        event.getId(),
                        "곧 시작하는 이벤트: " + event.getTitle(),
                        message
                );
            }
        }

        // PrivateTodo 알림
        List<PrivateTodo> upcomingTodos = privateTodoRepository.findTodosStartingAfterOffset(now);

        for (PrivateTodo todo : upcomingTodos) {
            LocalDateTime reminderTime = todo.getDate().minusMinutes(todo.getOffsetMinutes());
            if (now.isBefore(reminderTime) || now.isAfter(reminderTime.plusMinutes(1))) continue;

            boolean alreadySent = notificationRepository.existsByUserIdAndReferenceIdAndType(
                    todo.getOwnerId(), todo.getId(), "PRIVATE_TODO"
            );
            if (alreadySent) continue;

            String message = todo.getOffsetMinutes() == 0
                    ? todo.getTitle() + " TODO가 지금 시작합니다."
                    : todo.getTitle() + " TODO가 " + todo.getOffsetMinutes() + "분 후 시작합니다.";

            notificationService.sendNotification(
                    todo.getOwnerId(),
                    "PRIVATE_TODO",
                    todo.getId(),
                    "곧 시작하는 TODO: " + todo.getTitle(),
                    message
            );
        }
    }

    /**
     * 이벤트 시간 변경 시 호출
     * 기존 미발송 알림 삭제 후 offset 기준 새 알림 생성
     */
    @Transactional
    public void handleEventTimeChange(Event event) {
        // 기존 미발송 알림 삭제
        notificationRepository.deleteAllByReferenceIdAndTypeAndIsReadFalse(event.getId(), "EVENT");

        // 이벤트 멤버 가져오기
        List<User> members = eventMemberRepository.findUsersByEventId(event.getId());

        for (User member : members) {
            String message = event.getOffsetMinutes() == 0
                    ? event.getTitle() + " 이벤트가 지금 시작합니다."
                    : event.getTitle() + " 이벤트가 " + event.getOffsetMinutes() + "분 후 시작합니다.";

            notificationService.sendNotification(
                    member.getId(),
                    "EVENT",
                    event.getId(),
                    "곧 시작하는 이벤트: " + event.getTitle(),
                    message
            );
        }
    }

    /**
     * PrivateTodo 시간 변경 시 호출
     * 기존 미발송 알림 삭제 후 offset 기준 새 알림 생성
     */
    @Transactional
    public void handlePrivateTodoTimeChange(PrivateTodo todo) {
        notificationRepository.deleteAllByReferenceIdAndTypeAndIsReadFalse(todo.getId(), "PRIVATE_TODO");
        String message = todo.getOffsetMinutes() == 0
                ? todo.getTitle() + " TODO가 지금 시작합니다."
                : todo.getTitle() + " TODO가 " + todo.getOffsetMinutes() + "분 후 시작합니다.";

        notificationService.sendNotification(
                todo.getOwnerId(),
                "PRIVATE_TODO",
                todo.getId(),
                "곧 시작하는 TODO: " + todo.getTitle(),
                message
        );
    }
}
