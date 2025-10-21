package cola.springboot.cocal.project;

import cola.springboot.cocal.common.api.ApiResponse;
import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final UserRepository userRepository;

    // 프로젝트 생성
    @PostMapping()
    public ResponseEntity<ApiResponse<ProjectResponseDto>> createProject(
            @RequestBody ProjectRequestDto request,
            Authentication authentication,
            HttpServletRequest httpReq) {

        Long userId = Long.parseLong(authentication.getName()); // userId 가져오기
        ProjectResponseDto data = projectService.createProject(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }


    // project 목록 조회
    @GetMapping()
    public ResponseEntity<ApiResponse<Page<ProjectResponseDto>>> getMyProjects(
            // 쿼리 파라미터로 page와 size를 받음, 없으면 기본값 사용
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Authentication authentication,
            HttpServletRequest httpReq
    ) {
        // Pageable 객체 생성: 페이지 번호, 페이지 크기, 정렬 기준 포함
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // principal이 Long이면 이렇게 캐스팅
        Long userId = (Long) authentication.getPrincipal();

        // 이메일은 UserRepository에서 꺼내기
        String email = userRepository.findById(userId)
                .map(User::getEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Service 호출 → DB에서 페이지 단위로 ProjectResponseDto 리스트 가져오기
        Page<ProjectResponseDto> data = projectService.getProjects(userId, email, pageable);

        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

    // project 개별 정보 조회
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponseDto>> getProjectById(
            @PathVariable("projectId") Long projectId,
            Authentication authentication,
            HttpServletRequest httpReq
    ) {
        Long userId = Long.parseLong(authentication.getName());
        String email = userRepository.findById(userId)
                .map(User::getEmail)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "사용자를 찾을 수 없습니다."
                ));

        ProjectResponseDto data = projectService.getProjectById(projectId, userId, email);
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

    // project update
    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponseDto>> updateProject(
            @PathVariable("projectId") Long projectId,  // 이름 명시
            @Valid @RequestBody ProjectRequestDto request,
            Authentication authentication,
            HttpServletRequest httpReq) {

        Long userId = Long.parseLong(authentication.getName());
        ProjectResponseDto data = projectService.updateProject(projectId, request, userId);
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

    // project delete
    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteProject(
            @PathVariable("projectId") Long projectId,
            Authentication authentication,
            HttpServletRequest httpReq) {

        Long userId = Long.parseLong(authentication.getName());
        projectService.deleteProject(projectId, userId);

        Map<String, String> data = Map.of("message", "프로젝트가 삭제되었습니다.");
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }
}


