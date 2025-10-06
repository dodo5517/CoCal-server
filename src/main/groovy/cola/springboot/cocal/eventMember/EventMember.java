package cola.springboot.cocal.eventMember;

import cola.springboot.cocal.event.Event;
import cola.springboot.cocal.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "event_members",
        indexes = {
                @Index(name = "idx_ep_user", columnList = "user_id")
        }
)
public class EventMember {

    @EmbeddedId
    private EventMemberId id;

    // 외래키 값을 복합키에 매핑
    @MapsId("eventId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreatedDate
    @Column(name = "created_at", nullable = false, columnDefinition = "timestamp(6)")
    private LocalDateTime createdAt;

    // 팩토리 메서드: 사용성 + 불변성 확보
    public static EventMember of(Event event, User user) {
        EventMember m = new EventMember();
        m.setEvent(event);
        m.setUser(user);
        m.setId(new EventMemberId(event.getId(), user.getId()));
        return m;
    }
}
