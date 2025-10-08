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

import java.util.List;

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

    /*
     * TODO 조회
     */
    // 개인 TODO 단건 조회
    @GetMapping("/todos/{todoId}")
    public ResponseEntity<ApiResponse<TodoResponse>> getPrivateTodo(
            @PathVariable("projectId") Long projectId,
            @PathVariable("todoId") Long todoId,
            Authentication authentication,
            HttpServletRequest httpReq
    ) {
        Long userId = Long.parseLong(authentication.getName());
        TodoResponse response = todoService.getPrivateTodo(projectId, userId, todoId);
        return ResponseEntity.ok(ApiResponse.ok(response, httpReq.getRequestURI()));
    }

    // 이벤트 TODO 단건 조회
    @GetMapping("/events/{eventId}/todos/{todoId}")
    public ResponseEntity<ApiResponse<TodoResponse>> getEventTodo(
            @PathVariable("projectId") Long projectId,
            @PathVariable("eventId") Long eventId,
            @PathVariable("todoId") Long todoId,
            Authentication authentication,
            HttpServletRequest httpReq
    ) {
        Long userId = Long.parseLong(authentication.getName());
        TodoResponse response = todoService.getEventTodo(projectId, userId, eventId, todoId);
        return ResponseEntity.ok(ApiResponse.ok(response, httpReq.getRequestURI()));
    }

    /**
     * TODO 수정
     * PUT /api/projects/{projectId}/todos/{todoId}
     */
    @PutMapping("/todos/{todoId}")
    public ResponseEntity<ApiResponse<TodoResponse>> updateTodo(
            @PathVariable("projectId") Long projectId,
            @PathVariable("todoId") Long todoId,
            @Valid @RequestBody TodoRequest request,
            Authentication authentication,
            HttpServletRequest httpReq
    ) {
        // 로그인된 사용자 ID
        Long userId = Long.parseLong(authentication.getName());

        // Todo 수정 서비스 호출
        TodoResponse response = todoService.updateTodo(projectId, userId, todoId, request);

        // Response 반환
        return ResponseEntity.ok(ApiResponse.ok(response, httpReq.getRequestURI()));
    }

}
