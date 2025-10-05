package cola.springboot.cocal.projectMember;

import cola.springboot.cocal.project.Project;
import cola.springboot.cocal.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "project_members")
public class ProjectMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role = MemberRole.MEMBER; // OWNER, ADMIN, MEMBER

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE; // 'ACTIVE','LEFT','KICKED','BLOCKED'

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum MemberRole {
        OWNER, ADMIN, MEMBER
    }
    public enum MemberStatus {
        ACTIVE, LEFT, KICKED, BLOCKED
    }
//  'ACTIVE',        -- 정상 참여 중
//  'LEFT',          -- 자진 탈퇴
//  'KICKED',        -- 강제 추방
//  'BLOCKED'        -- 일시 차단 (프로젝트 내 기능 제한)
}


