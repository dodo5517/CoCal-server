package cola.springboot.cocal.memo;

import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.memo.DTO.MemoCreateRequest;
import cola.springboot.cocal.memo.DTO.MemoMapper;
import cola.springboot.cocal.memo.DTO.MemoResponse;
import cola.springboot.cocal.project.Project;
import cola.springboot.cocal.project.ProjectRepository;
import cola.springboot.cocal.projectMember.ProjectMember;
import cola.springboot.cocal.projectMember.ProjectMemberRepository;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemoService {

    private final MemoRepository memoRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    // 메모 생성
    @Transactional
    public MemoResponse createMemo(Long projectId, Long userId, MemoCreateRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."
                ));

        // ACTIVE 멤버만 작성 허용
        boolean isMember = projectMemberRepository
                .existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 메모를 작성할 수 있습니다."
            );
        }

        // 작성자 존재 확인
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."
                ));

        // 내용 정제(앞뒤 공백 제거, 완전 공백 방지)
        String content = req.getContent() != null ? req.getContent().trim() : null;
        if (content == null || content.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CONTENT", "내용이 비어 있습니다.");
        }

        // 엔티티 생성/저장
        Memo memo = Memo.builder()
                .project(project)
                .memoDate(req.getMemoDate())
                .content(content)
                .author(author)
                .updatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        Memo saved = memoRepository.save(memo);
        return MemoMapper.toResponse(saved, author);
    }

    // 메모 조회
    @Transactional
    public Page<MemoResponse> getMemosByDate(Long projectId, Long requesterUserId, LocalDate date, Pageable pageable) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."
                ));
        // 권한: ACTIVE 멤버만
        boolean isMember = projectMemberRepository
                .existsByProjectIdAndUserIdAndStatus(projectId, requesterUserId, ProjectMember.MemberStatus.ACTIVE);

        if (!isMember) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 메모를 조회할 수 있습니다.");
        }

        Page<MemoResponse> page = memoRepository
                .findByProjectAndDate(projectId, date, pageable)
                .map(memo -> MemoMapper.toResponse(memo, memo.getAuthor()));

        return page;
    }
}