package cola.springboot.cocal.notification;

import cola.springboot.cocal.project.Project;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String type;

    @Column(name = "reference_id")
    private Long referenceId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", foreignKey = @ForeignKey(name = "fk_notifications_project"))
    private Project project;

    @Column(name = "project_name", length = 255)
    private String projectName;

    @Builder.Default
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "is_read")
    private Boolean isRead = false;
}
