package cola.springboot.cocal.todo.private_todo;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "private_todos",
        indexes = {
                @Index(name = "idx_pt_owner_due", columnList = "owner_id, date"),
                @Index(name = "idx_pt_project", columnList = "project_id")
        })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PrivateTodo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime date;

    @Column(length = 2048)
    private String url;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TodoStatus status = TodoStatus.IN_PROGRESS;

    @Column(name = "offset_minutes", nullable = false)
    private Integer offsetMinutes = 0;

    @Column(name = "order_no", nullable = false)
    private Integer orderNo = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum TodoStatus {
        IN_PROGRESS,
        DONE
    }
}
