package cola.springboot.cocal.notification;

import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.project.Project;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    // SSE용 emitter 저장소
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(60L * 1000 * 10); // 10분 타임아웃
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));

        // 연결 직후 간단한 ping 이벤트
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    // 알림 생성 후 실시간 전송
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResponse sendNotification(Long userId, String type, Long referenceId,
                                                 String title, String message,
                                                 Project project, String projectName) {

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .referenceId(referenceId)
                .title(title)
                .message(message)
                .project(project)
                .projectName(projectName)
                .isRead(false)
                .build();

        notificationRepository.saveAndFlush(notification);

        NotificationResponse response = NotificationResponse.fromEntity(notification);

        // SSE로 전송
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(response));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        }

        return response;
    }

    // 알림 읽음 처리
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "해당 알림을 찾을 수 없습니다."
                ));

        // 본인 확인
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "FORBIDDEN", "본인 알림만 읽음 처리할 수 있습니다."
            );
        }

        notification.setIsRead(true);
    }

    // 사용자 읽지 않은 알림 조회
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findAllByUserIdAndIsReadFalse(userId);
        return notifications.stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    // 전체 읽기
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId);
    }
}