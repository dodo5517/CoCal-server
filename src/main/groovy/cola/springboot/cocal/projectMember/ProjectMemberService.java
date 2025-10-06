package cola.springboot.cocal.projectMember;

import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.project.Project;
import cola.springboot.cocal.project.ProjectRepository;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    // 팀원 강제 추방
    @Transactional
    public String kick(Long actorUserId, Long projectId, Long targetUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "존재하지 않는 프로젝트입니다."
                ));

        // 권한: OWNER 또는 ADMIN
        boolean isOwner = project.getOwner().getId().equals(actorUserId);
        boolean isAdmin = projectMemberRepository.existsByProjectIdAndUserIdAndRole(
                projectId, actorUserId, ProjectMember.MemberRole.ADMIN);
        if (!isOwner && !isAdmin) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "INVITE_NOT_ALLOWED",
                    "프로젝트의 소유자 또는 관리자만 추방 권한이 있습니다.");
        }
        if (actorUserId.equals(targetUserId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "CANNOT_KICK_SELF", "자기 자신은 추방할 수 없습니다.");
        }

        // 멤버인지 확인
        Optional<User> targetUser = Optional.ofNullable(userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "사용자를 찾을 수 없습니다."
                )));
        ProjectMember pm = projectMemberRepository.findOne(projectId, targetUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "대상 멤버가 없습니다."));

        if ((pm.getRole() == ProjectMember.MemberRole.OWNER)||(pm.getRole() == ProjectMember.MemberRole.ADMIN)) {
            throw new BusinessException(HttpStatus.CONFLICT, "CANNOT_KICK_OWNER", "프로젝트 소유자 또는 관리자는 추방할 수 없습니다.");
        }

        pm.setStatus(ProjectMember.MemberStatus.KICKED);
        pm.setUpdatedAt(LocalDateTime.now());
        projectMemberRepository.save(pm);

        return String.format("'%s'님을 '%s'에서 추방했습니다.", targetUser.get().getName(), project.getName());
    }
}