package cola.springboot.cocal.memo;

import cola.springboot.cocal.common.api.ApiResponse;
import cola.springboot.cocal.memo.DTO.MemoCreateRequest;
import cola.springboot.cocal.memo.DTO.MemoResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/memos")
public class MemoController {

    private final MemoService memoService;

    // 메모 생성
    @PostMapping
    public ResponseEntity<ApiResponse<MemoResponse>> createMemo(@PathVariable Long projectId,
                                                                @Valid @RequestBody MemoCreateRequest req,
                                                                Authentication auth,
                                                                HttpServletRequest httpReq) {
        Long userId = Long.parseLong(auth.getName());
        MemoResponse data = memoService.createMemo(projectId, userId, req);
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }
}
