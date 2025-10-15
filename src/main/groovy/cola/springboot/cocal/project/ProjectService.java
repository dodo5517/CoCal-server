package cola.springboot.cocal.project;

import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.invite.InviteRepository;
import cola.springboot.cocal.projectMember.ProjectMember;
import cola.springboot.cocal.projectMember.ProjectMemberRepository;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final InviteRepository inviteRepository;

    // 프로젝트 생성
    @Transactional
    public ProjectResponseDto createProject(Long ownerId, ProjectRequestDto request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "사용자를 찾을 수 없습니다."
                ));

        // 종료일 검증: 오늘 이전이면 예외 발생
        if (request.getEndDate().isBefore(LocalDate.now())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_END_DATE",
                    "종료일은 오늘 이후여야 합니다."
            );
        }

        // 종료일 검증: 시작일보다 이전이면 예외 발생
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_RANGE",
                    "종료일은 시작일 이후여야 합니다."
            );
        }

        // db에 project 저장
        Project project = new Project();
        project.setName(request.getName());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setStatus(Project.Status.IN_PROGRESS);
        project.setOwner(owner);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        project.setDescription(request.getDescription());

        project = projectRepository.save(project);

        // 팀원 테이블에 OWNER로 등록
        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(owner)
                .role(ProjectMember.MemberRole.OWNER)
                .status(ProjectMember.MemberStatus.ACTIVE)
                .updatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        projectMemberRepository.save(member);

        // 멤버 정보 DTO
        ProjectMemberInfoDto memberInfo = ProjectMemberInfoDto.builder()
                .userId(owner.getId())
                .name(owner.getName())
                .email(owner.getEmail())
                .profileImageUrl(owner.getProfileImageUrl())
                .build();

        return ProjectResponseDto.builder()
                .id(project.getId())
                .name(project.getName())
                .ownerId(project.getOwner().getId())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .status(project.getStatus().name())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .description(project.getDescription())
                .members(List.of(memberInfo))
                .build();
    }

    // 목록 조회
    @Transactional
    public Page<ProjectResponseDto> getProjects(Long userId, String email, Pageable pageable) {
        // 오늘 날짜
        LocalDate today = LocalDate.now();
        // 프로젝트 조회
        Page<Project> page = projectRepository.findMyProjects(userId, email, pageable);
        // endDate 지났으면 COMPLETED로 저장
        page.getContent().forEach(project -> {
            if (project.getEndDate() != null
                    && project.getEndDate().isBefore(today)
                    && project.getStatus() != Project.Status.COMPLETED) {
                project.setStatus(Project.Status.COMPLETED);
                project.setUpdatedAt(LocalDateTime.now());
            }
        });

        return page.map(project -> {
            List<ProjectMemberInfoDto> members = projectMemberRepository.findActiveMembersWithUser(project.getId())
                    .stream()
                    .map(pm -> ProjectMemberInfoDto.builder()
                            .userId(pm.getUser().getId())
                            .name(pm.getUser().getName())
                            .email(pm.getUser().getEmail())
                            .profileImageUrl(pm.getUser().getProfileImageUrl())
                            .build())
                    .toList();

                    // ProjectResponseDto 매핑
                    return ProjectResponseDto.builder()
                            .id(project.getId())
                            .name(project.getName())
                            .ownerId(project.getOwner().getId())
                            .startDate(project.getStartDate())
                            .endDate(project.getEndDate())
                            .status(project.getStatus().name())
                            .createdAt(project.getCreatedAt())
                            .updatedAt(project.getUpdatedAt())
                            .description(project.getDescription())
                            .members(members)
                            .build();
                });
    }

    // 프로젝트 개별 조회
    @Transactional(readOnly = true)
    public ProjectResponseDto getProjectById(Long projectId, Long userId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "PROJECT_NOT_FOUND",
                        "존재하지 않는 프로젝트입니다."
                ));

        // 소유자 이거나 초대 받은 사용자 중 수락한 경우만
        boolean isOwner = project.getOwner().getId().equals(userId);
        boolean isAcceptedInvitee = inviteRepository.existsAcceptedInvite(projectId, email);

        if (!isOwner && !isAcceptedInvitee) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "프로젝트 접근 권한이 없습니다."
            );
        }

        // 팀원 목록
        List<ProjectMember> members = projectMemberRepository.findActiveMembersWithUser(projectId);
        List<ProjectMemberInfoDto> memberDtos = members.stream()
                .map(pm -> ProjectMemberInfoDto.builder()
                        .userId(pm.getUser().getId())
                        .name(pm.getUser().getName())
                        .email(pm.getUser().getEmail())
                        .profileImageUrl(pm.getUser().getProfileImageUrl())
                        .build())
                .toList();

        return ProjectResponseDto.builder()
            .id(project.getId())
            .name(project.getName())
            .ownerId(project.getOwner().getId())
            .startDate(project.getStartDate())
            .endDate(project.getEndDate())
            .status(project.getStatus().name())
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .description(project.getDescription())
            .members(memberDtos)
            .build();
    }

    // 프로젝트 수정
    @Transactional
    public ProjectResponseDto updateProject(Long projectId, ProjectRequestDto request, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "PROJECT_NOT_FOUND",
                        "존재하지 않는 프로젝트입니다."
                ));

        // 프로젝틋 소유자 확인
        if (!project.getOwner().getId().equals(userId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "본인 소유 프로젝트만 수정할 수 있습니다."
            );
        }

        // 종료일 검증
        if (request.getEndDate().isBefore(LocalDate.now())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_END_DATE",
                    "종료일은 오늘 이후여야 합니다."
            );
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_RANGE",
                    "종료일은 시작일 이후여야 합니다."
            );
        }

        // update
        project.setName(request.getName());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setStatus(request.getStatus() != null
                ? Project.Status.valueOf(request.getStatus())
                : project.getStatus());
        project.setUpdatedAt(LocalDateTime.now());
        project.setDescription(request.getDescription());

        project = projectRepository.save(project);

        // 멤버 정보 DTO
        List<ProjectMemberInfoDto> members = projectMemberRepository.findActiveMembersWithUser(project.getId())
                .stream()
                .map(pm -> ProjectMemberInfoDto.builder()
                        .userId(pm.getUser().getId())
                        .name(pm.getUser().getName())
                        .email(pm.getUser().getEmail())
                        .profileImageUrl(pm.getUser().getProfileImageUrl())
                        .build())
                .toList();

        return ProjectResponseDto.builder()
                .id(project.getId())
                .name(project.getName())
                .ownerId(project.getOwner().getId())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .status(project.getStatus().name())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .description(project.getDescription())
                .members(members)
                .build();
    }

    // 프로젝트 삭제
    @Transactional
    public void deleteProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "PROJECT_NOT_FOUND",
                        "프로젝트를 찾을 수 없습니다."
                ));

        // 소유자 확인
        if (!project.getOwner().getId().equals(userId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "본인 소유 프로젝트만 삭제할 수 있습니다."
            );
        }

        // 프로젝트 삭제
        projectRepository.delete(project);
    }
}
