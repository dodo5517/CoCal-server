package cola.springboot.cocal.invite;

import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.invite.dto.InviteListRequest;
import cola.springboot.cocal.invite.dto.InviteListResponse;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InviteQueryService {

    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;

    // 내가 받은 초대
    public Page<InviteListResponse> listMyInvites(Long userId, InviteListRequest req) {
        User me = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 유저입니다."));

        Pageable pageable = toPageable(req);
        Invite.InviteStatus status = parseStatus(req.getStatus());

        Page<Invite> page = inviteRepository.findMyInvitesWithJoins(
                me.getEmail(), status, pageable);

        return page.map(this::toDto);
    }
    // helpers
    private Pageable toPageable(InviteListRequest req) {
        String sortExpr = (req.getSort() != null ? req.getSort() : "createdAt,desc");
        String[] parts = sortExpr.split(",");
        String prop = parts[0];
        Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1]))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        if (!prop.equals("createdAt") && !prop.equals("expiresAt") && !prop.equals("status")) {
            prop = "createdAt";
        }
        return PageRequest.of(req.getPage(), req.getSize(), Sort.by(dir, prop));
    }
    private Invite.InviteStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Invite.InviteStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "잘못된 status 값: " + s);
        }
    }
    private InviteListResponse toDto(Invite i) {
        return InviteListResponse.builder()
                .id(i.getId())
                .projectId(i.getProject().getId())
                .projectName(i.getProject().getName())
                .email(i.getEmail())
                .status(i.getStatus().name())
                .createdAt(i.getCreatedAt())
                .expiresAt(i.getExpiresAt())
                .inviterEmail(i.getInvitedBy() != null ? i.getInvitedBy().getEmail() : null)
                .build();
    }
}
