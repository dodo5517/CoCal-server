package cola.springboot.cocal.invite.DTO.MemberListDto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InviteRow {
    private Long inviteId;
    private String email;
    private String status;     // PENDING/SENT/EXPIRED/DECLINED
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
