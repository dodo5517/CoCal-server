package cola.springboot.cocal.project;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;

    public Page<ProjectResponseDto> getProjects(Pageable pageable) {
        return projectRepository.findAll(pageable)
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
