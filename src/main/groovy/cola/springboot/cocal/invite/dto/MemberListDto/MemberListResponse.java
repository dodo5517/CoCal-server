package cola.springboot.cocal.invite.dto.MemberListDto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MemberListResponse {
    private List<MemberRow> members;
    private List<InviteRow> invites;
}
