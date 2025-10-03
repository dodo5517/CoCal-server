package cola.springboot.cocal.project;

import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public ResponseEntity<Project> createProject(@RequestBody Project project, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName()); // User ID 가져오기
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        project.setOwner(user); // owner에 로그인 사용자 세팅
        Project saved = projectRepository.save(project);
        return ResponseEntity.ok(saved);
    }

    // project 목록 조회
    @GetMapping()
    public ResponseEntity<Page<ProjectResponseDto>> getProjects(
            // 쿼리 파라미터로 page와 size를 받음, 없으면 기본값 사용
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        // Pageable 객체 생성: 페이지 번호, 페이지 크기, 정렬 기준 포함
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Service 호출 → DB에서 페이지 단위로 ProjectResponseDto 리스트 가져오기
        Page<ProjectResponseDto> projects = projectService.getProjects(pageable);
        return ResponseEntity.ok(projects);
    }

}


