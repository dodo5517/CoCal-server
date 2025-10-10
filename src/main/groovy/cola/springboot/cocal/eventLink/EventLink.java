package cola.springboot.cocal.eventLink;

import cola.springboot.cocal.event.Event;
import cola.springboot.cocal.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_links",
        uniqueConstraints = {
                @UniqueConstraint(name="uk_el_event_order", columnNames = {"event_id","order_no"}) // 켰다면
        },
        indexes = {
                @Index(name="idx_el_event", columnList="event_id"),
                @Index(name="idx_el_event_order", columnList="event_id,order_no")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EventLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name="url", nullable=false, length=2000)
    private String url;

    @Column(name="order_no", nullable=false)
    private Integer orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}