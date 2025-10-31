package cola.springboot.cocal.cal;

import cola.springboot.cocal.cal.DTO.ActiveDaysResponse;
import cola.springboot.cocal.cal.DTO.CalItemResponse;
import cola.springboot.cocal.cal.DTO.CalTodoResponse;
import cola.springboot.cocal.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cal/{projectId}")
public class CalController {
    private final CalService calService;

    // 캘린더 + 이벤트 + 메모 조회
    @GetMapping()
    public ResponseEntity<ApiResponse<CalItemResponse>> getCalendarItems(
            @PathVariable("projectId") Long projectId,
            Authentication authentication,
            HttpServletRequest httpReq) {
        Long userId = Long.parseLong(authentication.getName());
        CalItemResponse response = calService.getCalendarItems(userId, projectId);
        return ResponseEntity.ok(ApiResponse.ok(response, httpReq.getRequestURI()));
    }


    // TODO 조회
    @GetMapping("/todos")
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

    @GetMapping("/active-days")
    public ResponseEntity<ActiveDaysResponse> getActiveDays(
            @PathVariable("projectId") Long projectId,
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        ActiveDaysResponse response = calService.getActiveDays(userId, projectId, year, month);
        return ResponseEntity.ok(response);
    }

}
