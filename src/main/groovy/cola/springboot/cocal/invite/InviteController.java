package cola.springboot.cocal.invite;

import cola.springboot.cocal.common.api.ApiResponse;
import cola.springboot.cocal.invite.DTO.InviteResolveResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    // 초대 수락
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<Map<String, String>>> accept(Authentication auth, HttpServletRequest req,
                                                                   @RequestBody Map<String, String> body){
        Long userId = Long.parseLong(auth.getName());
        String token = body.get("token");
        inviteService.acceptLink(token, userId);
        Map<String, String> data = Map.of("message", "초대를 수락했습니다.");
        return ResponseEntity.ok(ApiResponse.ok(data, req.getRequestURI()));
    }
}
