package cola.springboot.cocal.invite.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteListRequest {
    private String status = "PENDING"; // 기본값은 pending
    private Integer page;
    private Integer size;
    private String sort = "createdAt,desc"; // 최신 순
}
