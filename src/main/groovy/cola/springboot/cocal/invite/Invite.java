package cola.springboot.cocal.invite;

import cola.springboot.cocal.user.User;
import cola.springboot.cocal.project.Project;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "invites",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_invite_project_email", columnNames = {"project_id", "email"}),
                @UniqueConstraint(name = "uk_invite_token", columnNames = {"token"})
        },
        indexes = {
                @Index(name = "idx_inv_project", columnList = "project_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 초대 대상 프로젝트
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_inv_project"))
    private Project project;

    @Column(nullable = false, length = 255)
    private String email;

    // 초대한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by",
            foreignKey = @ForeignKey(name = "fk_inv_inviter"))
    private User invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InviteStatus status = InviteStatus.PENDING;

    @Column(nullable = false, length = 64, unique = true)
    private String token;

    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // 상태 enum
    public enum InviteStatus {
        PENDING, ACCEPTED, DECLINED, EXPIRED, CANCEL
    }
}