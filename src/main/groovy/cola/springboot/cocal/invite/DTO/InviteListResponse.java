package cola.springboot.cocal.invite.DTO;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InviteListResponse {
    private Long id;
    private Long projectId;
    private String projectName;
    private String email;          // 초대 대상
    private String status;         // PENDING/ACCEPTED/DECLINED/EXPIRED
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String inviterEmail;    // 초대한 사람(없을 수 있음)
}
