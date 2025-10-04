package cola.springboot.cocal.project;

import cola.springboot.cocal.common.api.ApiResponse;
import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    // 프로젝트 생성
    @PostMapping()
    public ResponseEntity<ApiResponse<Project>> createProject(@RequestBody Project project, Authentication authentication,
                                                              HttpServletRequest httpReq) {
        Long userId = Long.parseLong(authentication.getName()); // User ID 가져오기
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "존재하지 않는 유저입니다."
                ));

        project.setOwner(user); // owner에 로그인 사용자 세팅
        Project data = projectRepository.save(project);
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }


    // project 목록 조회
    @GetMapping()
    public ResponseEntity<ApiResponse<Page<ProjectResponseDto>>> getProjects(
            // 쿼리 파라미터로 page와 size를 받음, 없으면 기본값 사용
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest httpReq
    ) {
        // Pageable 객체 생성: 페이지 번호, 페이지 크기, 정렬 기준 포함
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Service 호출 → DB에서 페이지 단위로 ProjectResponseDto 리스트 가져오기
        Page<ProjectResponseDto> data = projectService.getProjects(pageable);
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

}


