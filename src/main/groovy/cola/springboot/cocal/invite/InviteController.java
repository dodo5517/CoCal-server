package cola.springboot.cocal.invite;

import cola.springboot.cocal.common.api.ApiResponse;
import cola.springboot.cocal.invite.dto.InviteResolveResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invites")
public class InviteController {

    private final InviteService inviteService;

    // 초대 링크 확인(permitAll)
    @GetMapping("/resolve")
    public ResponseEntity<ApiResponse<InviteResolveResponse>> resolve(
            @RequestParam("token") String token,
            HttpServletRequest req
    ) {
        InviteResolveResponse data = inviteService.resolve(token);
        return ResponseEntity.ok(ApiResponse.ok(data, req.getRequestURI()));
    }
}
