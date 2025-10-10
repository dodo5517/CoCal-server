package cola.springboot.cocal.cal;

import cola.springboot.cocal.cal.DTO.CalTodoResponse;
import cola.springboot.cocal.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cal")
public class CalController {
    private final CalService calService;

    // 캘린더용 TODO 조회
    @GetMapping("/{projectId}/todos")
    public ResponseEntity<ApiResponse<List<CalTodoResponse>>> getTodos(
            @PathVariable("projectId") Long projectId,
            Authentication authentication,
            HttpServletRequest httpReq) {

        // 1. Authentication에서 사용자 ID 추출
        Long userId = Long.parseLong(authentication.getName());

        // 2. Service 호출
        List<CalTodoResponse> todos = calService.getTodosForCalendar(userId, projectId);
        return ResponseEntity.ok(ApiResponse.ok(todos, httpReq.getRequestURI()));
    }

}
