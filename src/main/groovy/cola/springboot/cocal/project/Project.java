package cola.springboot.cocal.project;

import cola.springboot.cocal.projectMember.ProjectMember;
import cola.springboot.cocal.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // 추가
    @Column(columnDefinition = "TEXT") // 길이 제한 없이 TEXT로 저장
    private String description; // nullable 가능

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    private List<ProjectMember> members = new ArrayList<>();

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

