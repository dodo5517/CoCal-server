package cola.springboot.cocal.invite.DTO;

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
    private String type;
    private String email; // 초대할 사람
    private String status;        // PENDING/ACCEPTED/DECLINED/EXPIRED
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Long invitedBy;       // 초대한 유저 id (nullable)
    private String inviteLink;

    public static InviteResponse of(Invite invite, String inviteLink) {
        return InviteResponse.builder()
                .id(invite.getId())
                .projectId(invite.getProject().getId())
                .type(invite.getType().name())
                .email(invite.getEmail())
                .status(invite.getStatus().name())
                .inviteLink(inviteLink)
                .build();
    }
}
