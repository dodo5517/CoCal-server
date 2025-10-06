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
@RequestMapping("/api/project/{projectId}/team")
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
}
