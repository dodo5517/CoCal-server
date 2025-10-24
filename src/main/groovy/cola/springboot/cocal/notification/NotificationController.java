package cola.springboot.cocal.notification;

import cola.springboot.cocal.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    // 읽지 않은 알림 조회
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(Authentication auth,
                                                                                  HttpServletRequest req) {
        Long userId = Long.parseLong(auth.getName()); // JWT 등에서 로그인 사용자 ID 가져오기
        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(ApiResponse.ok(notifications, req.getRequestURI()));
    }

    // 특정 사용자의 읽지 않은 알림 조회
    @GetMapping("/unread/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnread(
            @PathVariable("userId") Long userId,
            HttpServletRequest req)
    {
        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(ApiResponse.ok(notifications, req.getRequestURI()));
    }


    // 알림 읽음 처리
    @PostMapping("/read/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            Authentication auth,
            HttpServletRequest req
    ) {
        Long userId = Long.parseLong(auth.getName()); // JWT 등에서 사용자 ID 가져오기
        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok(ApiResponse.ok(null, req.getRequestURI()));
    }

    // 전체 읽기
    @PostMapping("/all-read")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(Authentication auth,
                                                              HttpServletRequest req) {
        Long userId = Long.parseLong(auth.getName()); // JWT 등에서 사용자 ID 가져오기
        int updatedCount = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.ok(updatedCount, req.getRequestURI()));
    }

}
