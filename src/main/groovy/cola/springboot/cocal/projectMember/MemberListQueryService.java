package cola.springboot.cocal.projectMember;

import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.invite.Invite;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.invite.InviteRepository;
import cola.springboot.cocal.invite.DTO.MemberListDto.InviteRow;
import cola.springboot.cocal.invite.DTO.MemberListDto.MemberListResponse;
import cola.springboot.cocal.invite.DTO.MemberListDto.MemberRow;
import cola.springboot.cocal.project.ProjectRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberListQueryService {
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final InviteRepository inviteRepository;

    public MemberListResponse getMemberList(Long requesterUserId, Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "존재하지 않는 프로젝트입니다."));

        // 멤버인지 확인
        Boolean exist = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, requesterUserId, ProjectMember.MemberStatus.ACTIVE);
        if (!exist) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "NO_MEMBERSHIP", "프로젝트 멤버가 아닙니다.");
        }

        // 멤버 목록  members.getUser().getEmail().toLowerCase()
        List<ProjectMember> members = projectMemberRepository.findActiveMembersWithUser(projectId);

        // 초대 목록 (PENDING 상태만)
        List<Invite.InviteStatus> showStatuses = List.of(Invite.InviteStatus.PENDING);
        List<Invite> invites = inviteRepository.findByProjectAndStatuses(projectId, showStatuses);

        // 멤버 이메일 집합 (중복 필터링용)
        Set<String> memberEmails = members.stream()
                .map(ProjectMember::getUser)
                .filter(Objects::nonNull)
                .map(User::getEmail)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // 매핑
        List<MemberRow> memberRows = members.stream()
                .map(pm -> MemberRow.builder()
                        .memberId(pm.getId())
                        .userId(pm.getUser().getId())
                        .name(pm.getUser().getName())
                        .email(pm.getUser().getEmail())
                        .avatarUrl(pm.getUser().getProfileImageUrl())
                        .role(pm.getRole().name())
                        .status(pm.getStatus().name())
                        .isMe(pm.getUser().getId().equals(requesterUserId))
                        .updatedAt(pm.getCreatedAt())
                        .build())
                .toList();
        List<InviteRow> inviteRows = invites.stream()
                .filter(i -> i.getEmail() != null)
                .filter(i -> !memberEmails.contains(i.getEmail().toLowerCase()))
                .map(i -> InviteRow.builder()
                        .inviteId(i.getId())
                        .email(i.getEmail())
                        .status(i.getStatus().name())
                        .createdAt(i.getCreatedAt())
                        .expiresAt(i.getExpiresAt())
                        .build())
                .toList();

        return MemberListResponse.builder()
                .members(memberRows)
                .invites(inviteRows)
                .build();
    }
}