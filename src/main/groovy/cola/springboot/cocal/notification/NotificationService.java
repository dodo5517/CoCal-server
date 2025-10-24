package cola.springboot.cocal.notification;

import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.project.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket용

    // 알림 생성 후 실시간 전송
    @Transactional
    public NotificationResponse sendNotification(Long userId, String type, Long referenceId, String title, String message, Project project, String projectName) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .referenceId(referenceId)
                .title(title)
                .message(message)
                .project(project) // 없으면 null로 받아옴
                .projectName(projectName)
                .isRead(false)
                .build();

        // DB 저장
        notificationRepository.save(notification);

        NotificationResponse response = NotificationResponse.fromEntity(notification);

        // WebSocket으로 실시간 전송
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, response);

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