package cola.springboot.cocal.invite.dto.MemberListDto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberRow {
    private Long memberId;
    private Long userId;
    private String name;
    private String email;
    private String avatarUrl;
    private String role;   // OWNER/ADMIN/MEMBER
    private String status; // ACTIVE/LEFT/...
    private boolean isMe;
    private LocalDateTime updatedAt;
}
