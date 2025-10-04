package cola.springboot.cocal.invite.dto;

import cola.springboot.cocal.invite.Invite;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
public class InviteResponse {
    private Long id;
    private Long projectId;
    private String email; // 초대할 사람
    private String status;        // PENDING/ACCEPTED/DECLINED/EXPIRED
    private String token;         // 이메일 전송/관리용(응답에서 숨기고 싶으면 null)
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Long invitedBy;       // 초대한 유저 id (nullable)

    public static InviteResponse of(Invite invite, boolean exposeToken) {
        return new InviteResponse(
                invite.getId(),
                invite.getProject().getId(),
                invite.getEmail(),
                invite.getStatus().name(),
                exposeToken ? invite.getToken() : null, // 보안상 토큰 숨길 수도 있음
                invite.getExpiresAt(),
                invite.getCreatedAt(),
                invite.getInvitedBy() != null ? invite.getInvitedBy().getId() : null
        );
    }
}
