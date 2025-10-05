package cola.springboot.cocal.invite;

import cola.springboot.cocal.common.api.ApiResponse;
import cola.springboot.cocal.invite.dto.InviteCreateRequest;
import cola.springboot.cocal.invite.dto.InviteListRequest;
import cola.springboot.cocal.invite.dto.InviteListResponse;
import cola.springboot.cocal.invite.dto.InviteResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/team")
public class TeamController {

    private final InviteService inviteService;
    private final InviteQueryService inviteQueryService;

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

    // 내가 받은 초대 목록 조회
    @GetMapping("/invite/me")
    public ResponseEntity<ApiResponse<Page<InviteListResponse>>> listMyInvites(
            Authentication authentication, HttpServletRequest httpReq,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Long userId = Long.parseLong(authentication.getName());
        InviteListRequest req = new InviteListRequest();
        req.setStatus(status);
        req.setPage(page);
        req.setSize(size);
        req.setSort(sort);

        Page<InviteListResponse> data = inviteQueryService.listMyInvites(userId, req);
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }
}
