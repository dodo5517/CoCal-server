package cola.springboot.cocal.projectMember;

import cola.springboot.cocal.invite.Invite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    Boolean existsByProjectIdAndUserId(Long projectId, Long userId);
    Boolean existsByProjectIdAndUserIdAndRole(Long projectId, Long userId, ProjectMember.MemberRole role);
}
