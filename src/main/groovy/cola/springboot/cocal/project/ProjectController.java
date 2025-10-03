package cola.springboot.cocal.project;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

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
