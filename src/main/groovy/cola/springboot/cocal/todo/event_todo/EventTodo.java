package cola.springboot.cocal.todo.event_todo;

import cola.springboot.cocal.event.Event;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_todos",
        indexes = {
                @Index(name = "idx_et_event", columnList = "event_id"),
                @Index(name = "idx_et_assignee", columnList = "author_id")
        })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class EventTodo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", insertable = false, updatable = false)
    private Event event;


    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 2048)
    private String url;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TodoStatus status = TodoStatus.IN_PROGRESS;

    @Column(name = "offset_minutes", nullable = false)
    private Integer offsetMinutes = 0;

    @Column(name = "author_id")
    private Long authorId;  // 선택적 담당자

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
