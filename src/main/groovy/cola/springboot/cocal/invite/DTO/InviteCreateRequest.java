package cola.springboot.cocal.invite.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InviteCreateRequest {
    // 초대할 사람
    @NotNull @Email
    private String email;

    // 만료일 며칠로 할지
    private Integer expireDays = 7;
}