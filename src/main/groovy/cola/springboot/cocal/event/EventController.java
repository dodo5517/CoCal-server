package cola.springboot.cocal.event;

import cola.springboot.cocal.common.api.ApiResponse;
import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.event.dto.EventCreateRequest;
import cola.springboot.cocal.event.dto.EventCreateResponse;
import cola.springboot.cocal.event.dto.EventResponse;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/events")
public class EventController {

    private final EventService eventService;
    private final UserRepository userRepository;

    // 이벤트 생성
    @PostMapping()
    public ResponseEntity<ApiResponse<EventCreateResponse>> createEvent(
            @PathVariable("projectId") Long projectId,
            @Valid @RequestBody EventCreateRequest request,
            Authentication authentication,
            HttpServletRequest httpReq // 로그인된 사용자 ID
    ) {
        Long userId = Long.parseLong(authentication.getName());

        String email = userRepository.findById(userId)
                .map(User::getEmail)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "사용자를 찾을 수 없습니다."
                ));

        EventCreateResponse data = eventService.createEvent(projectId, userId, email, request);
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

    // 이벤트(개별) 조회
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(
            @PathVariable("id") Long id,
            @PathVariable("projectId") Long projectId
    ) {
        // 서비스에서 이벤트 조회
        Event event = eventService.getEvent(id, projectId);

        // DTO 변환
        EventResponse eventResponse = EventResponse.fromEntity(event);

        // ApiResponse로 감싸기
        ApiResponse<EventResponse> response = ApiResponse.<EventResponse>builder()
                .success(true)
                .data(eventResponse)
                .error(null)
                .build();

        return ResponseEntity.ok(response);
    }
}
