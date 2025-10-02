package cola.springboot.cocal.project;

import cola.springboot.cocal.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    // 팀장
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_projects_owner"))
    private User owner;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // 사용자가 지정

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;   // 사용자가 지정

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.IN_PROGRESS;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Status {
        IN_PROGRESS,
        COMPLETED
    }

    @Builder
    public Project(String name, User owner, LocalDate startDate, LocalDate endDate, Status status) {
        this.name = name;
        this.owner = owner;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }
}

