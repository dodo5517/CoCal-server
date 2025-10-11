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
                .title(req.getTitle())
                .content(content)
                .url(req.getUrl())
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
    
    // 메모 수정
    @Transactional
    public MemoResponse updateMemo(Long projectId, Long memoId, Long requesterUserId, MemoCreateRequest req) {
        // 프로젝트 멤버 권한 확인
        boolean isMember = projectMemberRepository
                .existsByProjectIdAndUserIdAndStatus(projectId, requesterUserId, ProjectMember.MemberStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 수정할 수 있습니다.");
        }

        // 대상 메모 로드
        Memo memo = memoRepository.findByIdAndProjectIdWithAuthor(memoId, projectId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEMO_NOT_FOUND", "메모를 찾을 수 없습니다."));

        // 수정 권한: 작성자 본인 or OWNER/ADMIN
        boolean isAuthor = (memo.getAuthor() != null) && memo.getAuthor().getId().equals(requesterUserId);
        boolean isOwner = projectMemberRepository.existsByProjectIdAndUserIdAndRole(projectId, requesterUserId, ProjectMember.MemberRole.OWNER);
        boolean isAdmin = projectMemberRepository.existsByProjectIdAndUserIdAndRole(projectId, requesterUserId, ProjectMember.MemberRole.ADMIN);
        if (!(isAuthor || isOwner || isAdmin)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "MEMO_UPDATE_NOT_ALLOWED", "수정 권한이 없습니다.");
        }

        // 변경
        memo.setContent(req.getContent().trim());
        memo.setMemoDate(req.getMemoDate());

        // 저장 (DB 트리거가 updated_at을 갱신)
        Memo saved = memoRepository.save(memo);

        return MemoMapper.toResponse(saved, saved.getAuthor());
    }

    // 메모 삭제
    @Transactional
    public void deleteMemo(Long projectId, Long memoId, Long requesterUserId) {
        // 프로젝트 멤버 여부
        boolean isMember = projectMemberRepository
                .existsByProjectIdAndUserIdAndStatus(projectId, requesterUserId, ProjectMember.MemberStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 삭제할 수 있습니다.");
        }

        // 대상 메모
        Memo memo = memoRepository.findByIdAndProjectIdWithAuthor(memoId, projectId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEMO_NOT_FOUND", "메모를 찾을 수 없습니다."));

        User author = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."
                ));

        // 권한: 작성자 또는 OWNER/ADMIN
        boolean isAuthor = (memo.getAuthor() != null) && memo.getAuthor().getId().equals(requesterUserId);
        boolean isOwner = projectMemberRepository.existsByProjectIdAndUserIdAndRole(projectId, requesterUserId, ProjectMember.MemberRole.OWNER);
        boolean isAdmin = projectMemberRepository.existsByProjectIdAndUserIdAndRole(projectId, requesterUserId, ProjectMember.MemberRole.ADMIN);
        if (!(isAuthor || isOwner || isAdmin)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "MEMO_DELETE_NOT_ALLOWED", "삭제 권한이 없습니다.");
        }

        // 삭제
        memoRepository.delete(memo);
    }
}