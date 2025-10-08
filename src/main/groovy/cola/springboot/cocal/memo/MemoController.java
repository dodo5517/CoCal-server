package cola.springboot.cocal.memo;

import cola.springboot.cocal.common.api.ApiResponse;
import cola.springboot.cocal.memo.DTO.MemoCreateRequest;
import cola.springboot.cocal.memo.DTO.MemoResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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

    // 메모 조회
    @GetMapping
    public ResponseEntity<ApiResponse<Page<MemoResponse>>> listByDate(
            @PathVariable Long projectId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth,
            HttpServletRequest httpReq
    ) {
        Long userId = Long.parseLong(auth.getName());
        Page<MemoResponse> data = memoService.getMemosByDate(projectId, userId, date, pageable);
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }
}
