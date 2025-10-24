package cola.springboot.cocal.invite;

import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.invite.Invite.InviteStatus;
import cola.springboot.cocal.invite.Invite.InviteType;
import cola.springboot.cocal.invite.DTO.InviteCreateRequest;
import cola.springboot.cocal.invite.DTO.InviteResolveResponse;
import cola.springboot.cocal.invite.DTO.InviteResponse;
import cola.springboot.cocal.notification.Notification;
import cola.springboot.cocal.notification.NotificationResponse;
import cola.springboot.cocal.notification.NotificationService;
import cola.springboot.cocal.project.Project;
import cola.springboot.cocal.project.ProjectRepository;
import cola.springboot.cocal.projectMember.ProjectMember;
import cola.springboot.cocal.projectMember.ProjectMemberRepository;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.w3c.dom.views.AbstractView;

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
    private final InviteLinkBuilder inviteLinkBuilder;
    private final NotificationService notificationService;

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

    // 이메일로 프로젝트에 초대
    @Transactional
    public InviteResponse createEmailInvite(Long inviterUserId, Long projectId, InviteCreateRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "PROJECT_NOT_FOUND",
                        "존재하지 않는 프로젝트입니다."
                ));

        // 프로젝트 소유자 또는 관리자인지 확인
        boolean isOwner = project.getOwner().getId().equals(inviterUserId);
        boolean isAdmin = projectMemberRepository.existsByProjectIdAndUserIdAndRole(projectId, inviterUserId, ProjectMember.MemberRole.ADMIN);
        if (!isOwner && !isAdmin) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "INVITE_NOT_ALLOWED",
                    "프로젝트의 소유자만 초대할 수 있습니다."
            );
        }

        // 초대할 사람
        String email = req.getEmail() == null ? null : req.getEmail().toLowerCase();

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
        Optional<User> targetUser = Optional.ofNullable(userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "사용자를 찾을 수 없습니다."
                )));

        boolean alreadyMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, targetUser.get().getId(), ProjectMember.MemberStatus.ACTIVE);
        if (alreadyMember) {
            throw new BusinessException(HttpStatus.CONFLICT, "ALREADY_MEMBER", "이미 해당 프로젝트의 멤버입니다.");
        }

        // 타입 지정
        InviteType type = InviteType.EMAIL;

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
                        return InviteResponse.of(targetInv,null);
                    }
                    // 만료된 요청은 EXPIRED 설정
                    targetInv.setStatus(InviteStatus.EXPIRED);
                    targetInv.setUpdatedAt(LocalDateTime.now());
                    inviteRepository.saveAndFlush(targetInv);

                    // 만료된 경우는 EXPIRED로 간주하여 새로 생성
                    Invite saved = saveNewInvite(project, type, email, inviter, req.getExpireDays());
                    return InviteResponse.of(saved,null);
                }
                case CANCEL, EXPIRED, DECLINED -> {
                    Invite saved = saveNewInvite(project, type, email, inviter, req.getExpireDays());
                    return InviteResponse.of(saved,null);
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
        Invite saved = saveNewInvite(project, type, email, inviter, req.getExpireDays());

        // 알림 보내기: 초대받는 사람에게만
        if (type == Invite.InviteType.EMAIL) {
            NotificationResponse notification = notificationService.sendNotification(
                    targetUser.get().getId(),
                    "INVITE",
                    saved.getId(),
                    "팀 초대 알림",
                    inviter.getName() + "님이 '" + project.getName() + "' 프로젝트에 초대했습니다.",
                    saved.getProject(),
                    saved.getProject().getName()
            );

            System.out.println("Notification ID after flush: " + notification.getId());
        }

        return InviteResponse.of(saved,null);
    }
    private Invite saveNewInvite(Project project, InviteType type, String email, User inviter, Integer expireDays) {
        Invite newInv = Invite.builder()
                .project(project)
                .type(type) // OPEN_LINK일 경우 email nullable
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

    // 활성 링크 있는지 확인
    @Transactional
    public InviteResponse getOrCreateOpenLinkInvite(Long inviterUserId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "존재하지 않는 프로젝트입니다."
                ));

        // 권한: OWNER 또는 ADMIN
        boolean isOwner = project.getOwner().getId().equals(inviterUserId);
        boolean isAdmin = projectMemberRepository.existsByProjectIdAndUserIdAndRole(
                projectId, inviterUserId, ProjectMember.MemberRole.ADMIN);
        if (!isOwner && !isAdmin) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "INVITE_NOT_ALLOWED",
                    "프로젝트의 소유자 또는 관리자만 초대 링크를 생성할 수 있습니다.");
        }

        // 활성 링크 있으면 재사용
        Optional<Invite> active = inviteRepository.findActiveOpenLink(projectId);
        if (active.isPresent()) {
            String link = inviteLinkBuilder.build(active.get().getToken());
            return InviteResponse.of(active.get(), link);
        }

        // 없으면 생성 (아래 트랜잭션 열고 저장 필요)
        return createOpenLinkInvite(inviterUserId, project, 7);
    }

    // 공유 링크 생성
    @Transactional
    protected InviteResponse createOpenLinkInvite(Long inviterUserId, Project project, Integer expireDays) {
        User inviter = userRepository.findById(inviterUserId).orElse(null);

        // 타입 지정
        InviteType type = InviteType.OPEN_LINK;
        
        // 활성 초대 레코드 생성
        Invite inv = saveNewInvite(project, type, null, inviter, expireDays);
        // 인덱스 충돌 즉시 확인
        Invite saved = inviteRepository.saveAndFlush(inv);
        String link = inviteLinkBuilder.build(saved.getToken());
        return InviteResponse.of(saved, link);
    }

    // 초대함에서 초대 수락
    @Transactional
    public void acceptInvite(Long inviteId, Long userId) {
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "INVITE_NOT_FOUND", "초대를 찾을 수 없습니다."));

        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVITE_ALREADY_HANDLED", "이미 처리된 초대입니다.");
        }

        // 이미 멤버인지 확인
        boolean alreadyMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(invite.getProject().getId(), userId, ProjectMember.MemberStatus.ACTIVE);
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

    // 초대함에서 초대 거절
    @Transactional
    public void declineInvite(Long inviteId, Long userId) {
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "INVITE_NOT_FOUND", "초대를 찾을 수 없습니다."));

        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVITE_ALREADY_HANDLED", "이미 처리된 초대입니다.");
        }

        // 이미 멤버인지 확인
        boolean alreadyMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(invite.getProject().getId(), userId, ProjectMember.MemberStatus.ACTIVE);
        if (alreadyMember) {
            throw new BusinessException(HttpStatus.CONFLICT, "ALREADY_MEMBER", "이미 해당 프로젝트의 멤버입니다.");
        }

        // 초대 상태 업데이트
        invite.setStatus(InviteStatus.DECLINED);
    }

    // 초대 링크 확인 후 정보 조회
    @Transactional
    public InviteResolveResponse resolve(String rawToken) {
        String token = rawToken == null ? "" : rawToken.trim();
        if (token.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "토큰이 비어 있습니다.");
        }

        // 토큰으로 초대 링크 조회
        Invite inv = inviteRepository.findByTokenWithJoins(token)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "INVITE_NOT_FOUND", "유효하지 않은 초대입니다."
                ));

        // 실제 상태 계산(만료/사용초과/취소 등)
        InviteEffectiveStatus effective = computeEffectiveStatus(inv);

        // 프론트가 안내를 위해 필요한 최소 정보만 노출 (과노출 금지)
        return InviteResolveResponse.builder()
                .inviteId(inv.getId())
                .projectId(inv.getProject().getId())
                .projectName(inv.getProject().getName())
                .invitedByEmail(inv.getInvitedBy() != null ? inv.getInvitedBy().getEmail() : null)
                .type(inv.getType().name())        // EMAIL or OPEN_LINK
                .status(effective.name())          // PENDING / EXPIRED / CANCELLED / LIMIT_REACHED 등
                .expiresAt(inv.getExpiresAt())
                .message(makeMessage(effective, inv))
                .build();
    }

    private InviteEffectiveStatus computeEffectiveStatus(Invite inv) {
        // 취소된 경우
        if (inv.getStatus() == Invite.InviteStatus.CANCEL) return InviteEffectiveStatus.CANCEL;

        // 만료됐는지 확인 -> 시간 지났으면 만료 처리
        LocalDateTime now = LocalDateTime.now();
        if (inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(now)) return InviteEffectiveStatus.EXPIRED;
        // 만료된 경우
        if (inv.getStatus() == Invite.InviteStatus.EXPIRED) return InviteEffectiveStatus.EXPIRED;

        // 이미 수락된 단건 초대(EMAIL 타입 등)면 유효 아님으로 취급할지 판단
        if (inv.getType() == Invite.InviteType.EMAIL && inv.getStatus() == Invite.InviteStatus.ACCEPTED) {
            return InviteEffectiveStatus.ALREADY_ACCEPTED;
        }

        // 그 외는 PENDING
        return InviteEffectiveStatus.PENDING;
    }
    private String makeMessage(InviteEffectiveStatus st, Invite inv) {
        return switch (st) {
            case PENDING -> String.format("'%s' 프로젝트에 초대되었습니다.", inv.getProject().getName());
            case EXPIRED -> "초대가 만료되었습니다.";
            case CANCEL -> "취소된 초대입니다.";
            case ALREADY_ACCEPTED -> "이미 처리된 초대입니다. 새 초대 링크를 받아 다시 시도해주세요.";
        };
    }
    // 화면 표시에 쓰는 효과 상태(응답 전용)
    public enum InviteEffectiveStatus {
        PENDING, EXPIRED, CANCEL, ALREADY_ACCEPTED
    }

    // 링크 초대 수락
    @Transactional
    public void acceptLink(String token, Long userId) {
        // 토큰으로 초대 링크 조회
        Invite inv = inviteRepository.findByTokenWithJoins(token)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "INVITE_NOT_FOUND", "유효하지 않은 초대입니다."
                ));

        // 만료/취소 검증
        if (inv.getStatus() == InviteStatus.CANCEL) throw new BusinessException(HttpStatus.GONE, "INVITE_CANCELLED", "초대가 취소되었습니다.");
        if ((inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(LocalDateTime.now())|(inv.getStatus() == Invite.InviteStatus.EXPIRED)))
            throw new BusinessException(HttpStatus.GONE, "INVITE_EXPIRED", "초대가 만료되었습니다.");

        Long projectId = inv.getProject().getId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "사용자를 찾을 수 없습니다."
                ));

        // 이미 멤버면 멱등 OK
        if (!projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE)) {
            ProjectMember member = ProjectMember.builder()
                    .project(inv.getProject())
                    .user(user)
                    .role(ProjectMember.MemberRole.MEMBER)
                    .status(ProjectMember.MemberStatus.ACTIVE)
                    .updatedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
            projectMemberRepository.save(member);
        }

        inv.setType(InviteType.EMAIL);
        inv.setEmail(user.getEmail());
        inv.setStatus(InviteStatus.ACCEPTED);
        inv.setUpdatedAt(LocalDateTime.now());
    }
}
