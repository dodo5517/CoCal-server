package cola.springboot.cocal.invite.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteResolveResponse {
    private Long inviteId;
    private Long projectId;
    private String projectName;
    private String invitedByEmail; // null 가능
    private String type;           // "EMAIL" | "OPEN_LINK"
    private String status;         // "PENDING" | "EXPIRED" | ...
    private LocalDateTime expiresAt;
    private String message;        // 프론트 안내문
}