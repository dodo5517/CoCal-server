package cola.springboot.cocal.invite;

import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.invite.Invite.InviteStatus;
import cola.springboot.cocal.invite.dto.InviteCreateRequest;
import cola.springboot.cocal.invite.dto.InviteResponse;
import cola.springboot.cocal.project.Project;
import cola.springboot.cocal.project.ProjectRepository;
import cola.springboot.cocal.projectMember.ProjectMember;
import cola.springboot.cocal.projectMember.ProjectMemberRepository;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;

    // 초대 만료일 기본값 = 7일
    private static final int DEFAULT_EXPIRE_DAYS = 7;
    private static final SecureRandom RNG = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    // 토큰 생성기
    private String newToken() {
        byte[] buf = new byte[32]; // 256bit -> 64 hex
        RNG.nextBytes(buf);
        return HEX.formatHex(buf);
    }

    // 초대 만료 날짜 생성기
    private LocalDateTime calcExpireAt(Integer days) {
        int d = (days == null || days <= 0) ? DEFAULT_EXPIRE_DAYS : days;
        return LocalDateTime.now().plusDays(d);
    }

    // 프로젝트에 초대
    @Transactional
    public InviteResponse createInvite(Long inviterUserId, Long projectId, InviteCreateRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "PROJECT_NOT_FOUND",
                        "존재하지 않는 프로젝트입니다."
                ));

        // 프로젝트 소유자(팀장)인지 확인
        if (!project.getOwner().getId().equals(inviterUserId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "NOT_PROJECT_OWNER",
                    "해당 프로젝트의 소유자만 초대를 생성할 수 있습니다."
            );
        }

        // 초대할 사람
        String email = req.getEmail().toLowerCase().trim();

        // 초대한 사람
        User inviter = userRepository.findById(inviterUserId)
                .orElse(null); // DB FK는 SET NULL 허용. 없는 경우도 null 저장 가능.

        // DECLINED 3회 이상 선체크 후 바로 차단
        long declinedCount = inviteRepository.countDeclinedInvites(project.getId(), email);
        if (declinedCount >= 3) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "INVITE_BLOCKED",
                    "해당 사용자는 이미 3회 이상 초대를 거절했습니다. 더 이상 초대할 수 없습니다."
            );
        }

        // 이미 멤버인지 선체크(있다면 초대 불필요) 하는 부분 팀원 테이블 만들면 추가.

        // 이미 초대된 이메일인지 확인(가장 최신 건으로)
        Optional<Invite> existing =
                inviteRepository.findTopByProject_IdAndEmailIgnoreCaseOrderByCreatedAtDesc(project.getId(), email);

        // 이미 초대된 적이 있다면 상태에 따라 분기 처리
        if (existing.isPresent()) {
            Invite targetInv = existing.get();

            switch (targetInv.getStatus()) {
                case PENDING -> {
                    // 만료되지 않았으면 기존 초대 재사용
                    if (targetInv.getExpiresAt() == null || targetInv.getExpiresAt().isAfter(LocalDateTime.now())) {
                        return InviteResponse.of(targetInv);
                    }
                    // 만료된 요청은 EXPIRED 설정
                    targetInv.setStatus(InviteStatus.EXPIRED);
                    targetInv.setUpdatedAt(LocalDateTime.now());

                    // 만료된 경우는 EXPIRED로 간주하여 새로 생성
                    Invite saved = saveNewInvite(project, email, inviter, req.getExpireDays());
                    return InviteResponse.of(saved);
                }
                case CANCEL, EXPIRED, DECLINED -> {
                    Invite saved = saveNewInvite(project, email, inviter, req.getExpireDays());
                    return InviteResponse.of(saved);
                }
                case ACCEPTED -> throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "INVITE_ALREADY_ACCEPTED",
                        "이미 수락된 초대입니다."
                );
                default -> throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_INVITE_STATUS",
                        "처리할 수 없는 초대 상태입니다: " + targetInv.getStatus()
                );
            }
        }
        // 기존 초대가 없는 경우 새 초대 생성
        Invite saved = saveNewInvite(project, email, inviter, req.getExpireDays());
        return InviteResponse.of(saved);
    }
    private Invite saveNewInvite(Project project, String email, User inviter, Integer expireDays) {
        Invite newInv = Invite.builder()
                .project(project)
                .email(email)
                .invitedBy(inviter)
                .status(InviteStatus.PENDING)
                .token(newToken())
                .expiresAt(calcExpireAt(expireDays))
                .updatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        return inviteRepository.save(newInv);
    }

    // 초대 수락
    @Transactional
    public void acceptInvite(Long inviteId, Long userId) {
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "INVITE_NOT_FOUND", "초대를 찾을 수 없습니다."));

        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVITE_ALREADY_HANDLED", "이미 처리된 초대입니다.");
        }

        // 이미 멤버인지 확인
        boolean alreadyMember = projectMemberRepository.existsByProjectIdAndUserId(invite.getProject().getId(), userId);
        if (alreadyMember) {
            throw new BusinessException(HttpStatus.CONFLICT, "ALREADY_MEMBER", "이미 해당 프로젝트의 멤버입니다.");
        }

        // 초대 상태 업데이트
        invite.setStatus(InviteStatus.ACCEPTED);

        // 팀원 등록
        ProjectMember member = ProjectMember.builder()
                .project(invite.getProject())
                .user(userRepository.findById(userId).orElseThrow())
                .role(ProjectMember.MemberRole.MEMBER)
                .status(ProjectMember.MemberStatus.ACTIVE)
                .updatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        projectMemberRepository.save(member);
    }
}
