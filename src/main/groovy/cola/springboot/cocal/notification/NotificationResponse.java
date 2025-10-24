package cola.springboot.cocal.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String message;
    private Long referenceId;
    private Long projectId;
    private String projectName;
    private Boolean isRead;
    private LocalDateTime sentAt;

    public static NotificationResponse fromEntity(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .projectId(n.getProject() != null ? n.getProject().getId() : null)
                .projectName(n.getProjectName() != null ? n.getProject().getName() : null)
                .isRead(n.getIsRead())
                .sentAt(n.getSentAt())
                .build();
    }
}
