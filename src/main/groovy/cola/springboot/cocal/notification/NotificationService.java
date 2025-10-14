package cola.springboot.cocal.notification;

import cola.springboot.cocal.common.exception.BusinessException;
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
    public Notification sendNotification(Long userId, String type, Long referenceId, String title, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .referenceId(referenceId)
                .title(title)
                .message(message)
                .isRead(false)
                .build();

        // DB 저장
        notificationRepository.save(notification);

        // WebSocket으로 실시간 전송
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, notification);

        return notification;
    }

    // 알림 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "해당 알림을 찾을 수 없습니다."
                ));
        notification.setIsRead(true);
    }

    // 사용자 읽지 않은 알림 조회
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findAllByUserIdAndIsReadFalse(userId);
    }
}