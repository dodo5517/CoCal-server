package cola.springboot.cocal.project;

import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    // 프로젝트 생성
    @Transactional
    public ProjectResponseDto createProject(Long ownerId, ProjectRequestDto request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "사용자를 찾을 수 없습니다."
                ));

        Project project = new Project();
        project.setName(request.getName());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setStatus(Project.Status.IN_PROGRESS);
        project.setOwner(owner);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        project = projectRepository.save(project);

        return ProjectResponseDto.builder()
                .id(project.getId())
                .name(project.getName())
                .ownerId(project.getOwner().getId())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .status(project.getStatus().name())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    // 목록 조회
    @Transactional(readOnly = true)
    public Page<ProjectResponseDto> getProjects(Long userId, String email, Pageable pageable) {
        return projectRepository.findMyProjects(userId, email, pageable)
                .map(project -> ProjectResponseDto.builder()
                        .id(project.getId())
                        .name(project.getName())
                        .ownerId(project.getOwner().getId())
                        .startDate(project.getStartDate())
                        .endDate(project.getEndDate())
                        .status(project.getStatus().name())
                        .createdAt(project.getCreatedAt())
                        .updatedAt(project.getUpdatedAt())
                        .build());
    }

}
