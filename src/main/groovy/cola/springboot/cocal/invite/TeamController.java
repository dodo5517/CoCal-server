package cola.springboot.cocal.invite;

import cola.springboot.cocal.common.api.ApiResponse;
import cola.springboot.cocal.invite.DTO.InviteCreateRequest;
import cola.springboot.cocal.invite.DTO.InviteListRequest;
import cola.springboot.cocal.invite.DTO.InviteListResponse;
import cola.springboot.cocal.invite.DTO.InviteResponse;
import cola.springboot.cocal.invite.DTO.MemberListDto.MemberListResponse;
import cola.springboot.cocal.projectMember.MemberListQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/team")
public class TeamController {

    private final InviteService inviteService;
    private final InviteQueryService inviteQueryService;
    private final MemberListQueryService memberListQueryService;

    // 이메일로 초대 생성
    @PostMapping("/{projectId}/invites-email")
    public ResponseEntity<ApiResponse<InviteResponse>> createEmail(@PathVariable Long projectId,
                                                              @Valid @RequestBody InviteCreateRequest req,
                                                              HttpServletRequest httpReq,
                                                              Authentication auth) {
        Long inviterUserId = Long.parseLong(auth.getName());
        InviteResponse data = inviteService.createEmailInvite(inviterUserId, projectId, req);
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

    // 초대 링크 복사(생성)
    @GetMapping("/{projectId}/invites-link")
    public ResponseEntity<ApiResponse<InviteResponse>> createLink(@PathVariable Long projectId,
                                                              HttpServletRequest httpReq,
                                                              Authentication auth) {
        Long inviterUserId = Long.parseLong(auth.getName());
        InviteResponse data = inviteService.getOrCreateOpenLinkInvite(inviterUserId, projectId);
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

    // 내가 받은 초대 목록 조회
    @GetMapping("/invites/me")
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

    // 프로젝트의 팀원/초대 현황 조회(초대는 PENDING만 조회)
    @GetMapping("/{projectId}/list")
    public ResponseEntity<ApiResponse<MemberListResponse>> getMemberList(
            @PathVariable Long projectId,
            Authentication auth,
            HttpServletRequest httpReq) {

        Long requesterUserId = Long.parseLong(auth.getName());
        MemberListResponse data = memberListQueryService.getMemberList(requesterUserId, projectId);
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

    // 초대 수락
    @PostMapping("/invites/{inviteId}/accept")
    public ResponseEntity<ApiResponse<Map<String, String>>> acceptInvite(
            @PathVariable Long inviteId,
            Authentication auth,
            HttpServletRequest req
    ) {
        Long userId = Long.parseLong(auth.getName());
        inviteService.acceptInvite(inviteId, userId);
        Map<String, String> data = Map.of("message", "초대를 수락했습니다.");
        return ResponseEntity.ok(ApiResponse.ok(data, req.getRequestURI()));
    }

    // 초대 거절
    @PostMapping("/invites/{inviteId}/decline")
    public ResponseEntity<ApiResponse<Map<String, String>>> declineInvite(
            @PathVariable Long inviteId,
            Authentication auth,
            HttpServletRequest req
    ) {
        Long userId = Long.parseLong(auth.getName());
        inviteService.declineInvite(inviteId, userId);
        Map<String, String> data = Map.of("message", "초대를 거절했습니다.");
        return ResponseEntity.ok(ApiResponse.ok(data, req.getRequestURI()));
    }
}
