package cola.springboot.cocal.invite;

import cola.springboot.cocal.common.api.ApiResponse;
import cola.springboot.cocal.invite.dto.InviteCreateRequest;
import cola.springboot.cocal.invite.dto.InviteResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/team")
public class TeamController {

    private final InviteService inviteService;

    // 초대 생성
    @PostMapping("/{projectId}/invite")
    public ResponseEntity<ApiResponse<InviteResponse>> create(@PathVariable Long projectId,
                                                              @Valid @RequestBody InviteCreateRequest req,
                                                              HttpServletRequest httpReq,
                                                              Authentication auth) {
        Long inviterUserId = Long.parseLong(auth.getName());
        InviteResponse data = inviteService.createInvite(inviterUserId, projectId, req);
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }
}
