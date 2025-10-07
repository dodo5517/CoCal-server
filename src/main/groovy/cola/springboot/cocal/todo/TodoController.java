package cola.springboot.cocal.todo;

import cola.springboot.cocal.common.api.ApiResponse;
import cola.springboot.cocal.todo.dto.TodoRequest;
import cola.springboot.cocal.todo.dto.TodoResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}")
public class TodoController {
    private final TodoService todoService;

    /**
     * TODO 생성
     * POST /api/projects/{projectId}/todos
     */
    @PostMapping("/todos")
    public ResponseEntity<ApiResponse<TodoResponse>> createTodo(
            @PathVariable("projectId") Long projectId,
            Authentication authentication, // 로그인된 사용자
            @Valid @RequestBody TodoRequest request,
            HttpServletRequest httpReq
    ) {
        // userId는 로그인 정보에서 가져오기
        Long userId = Long.parseLong(authentication.getName()); // userId 가져오기

        TodoResponse response = todoService.createTodo(projectId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, httpReq.getRequestURI()));
    }
}
