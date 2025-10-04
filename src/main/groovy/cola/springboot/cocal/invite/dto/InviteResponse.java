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
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Long invitedBy;       // 초대한 유저 id (nullable)

    public static InviteResponse of(Invite invite) {
        return new InviteResponse(
                invite.getId(),
                invite.getProject().getId(),
                invite.getEmail(),
                invite.getStatus().name(),
                invite.getExpiresAt(),
                invite.getCreatedAt(),
                invite.getInvitedBy() != null ? invite.getInvitedBy().getId() : null
        );
    }
}
