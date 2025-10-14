package cola.springboot.cocal.projectMember;

import cola.springboot.cocal.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/team")
public class ProjectMemberController {
    private final ProjectMemberService memberService;

    // 프로젝트 팀원 강제추방
    @PostMapping("/{userId}/kick")
    public ResponseEntity<ApiResponse<Map<String, String>>> kick(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            Authentication auth,
            HttpServletRequest httpReq
    ) {
        Long actorUserId = Long.parseLong(auth.getName());
        Map<String, String> data = Map.of("message", memberService.kick(actorUserId, projectId, userId));
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

    // 프로젝트 나가기(자진 탈퇴)
    @PostMapping("/leave")
    public ResponseEntity<ApiResponse<Map<String, String>>> leave(
            @PathVariable Long projectId,
            Authentication auth,
            HttpServletRequest httpReq) {

        Long actorUserId = Long.parseLong(auth.getName());
        Map<String, String> data = Map.of("message", memberService.leaveProject(actorUserId, projectId));
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

    @PostMapping("/invites/{inviteId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> cancelInvite(
            @PathVariable Long projectId,
            @PathVariable Long inviteId,
            Authentication auth,
            HttpServletRequest httpReq
    ) {
        Long actorUserId = Long.parseLong(auth.getName());
        Map<String, String> data = Map.of("message", memberService.cancelInvite(actorUserId, projectId, inviteId));
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }
}
