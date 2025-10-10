package cola.springboot.cocal.cal;

import cola.springboot.cocal.cal.DTO.CalItemResponse;
import cola.springboot.cocal.cal.DTO.CalTodoResponse;
import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.event.Event;
import cola.springboot.cocal.event.EventRepository;
import cola.springboot.cocal.event.dto.EventResponse;
import cola.springboot.cocal.eventMember.EventMemberRepository;
import cola.springboot.cocal.memo.DTO.MemoMapper;
import cola.springboot.cocal.memo.DTO.MemoResponse;
import cola.springboot.cocal.memo.Memo;
import cola.springboot.cocal.memo.MemoRepository;
import cola.springboot.cocal.project.Project;
import cola.springboot.cocal.project.ProjectRepository;
import cola.springboot.cocal.projectMember.ProjectMember;
import cola.springboot.cocal.projectMember.ProjectMemberRepository;
import cola.springboot.cocal.todo.event_todo.EventTodo;
import cola.springboot.cocal.todo.event_todo.EventTodoRepository;
import cola.springboot.cocal.todo.private_todo.PrivateTodoRepository;
import cola.springboot.cocal.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalService {
    private final PrivateTodoRepository privateTodoRepository;
    private final EventTodoRepository eventTodoRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final MemoRepository memoRepository;

    // event, memo 조회(calendar 화면에서)
    @Transactional(readOnly = true)
    public CalItemResponse getCalendarItems(Long userId, Long projectId) {
        //  프로젝트의 시작일 가져오기
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        LocalDate startDate = project.getStartDate(); // Project 엔티티에 startDate 필드

        int year = startDate.getYear();
        int month = startDate.getMonthValue();
        int day = startDate.getDayOfMonth();

        // 프로젝트 존재 확인
        projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."));

        // 권한 체크
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 조회할 수 있습니다.");
        }

        // 이벤트 조회
        List<Event> events = eventRepository.findAllByProjectId(projectId);
        List<EventResponse> eventResponses = events.stream()
                .map(event -> {
                    List<User> members = eventMemberRepository.findUsersByEventId(event.getId());
                    return EventResponse.fromEntity(event, members);
                })
                .toList();

        // 메모 조회
        List<Memo> memos = memoRepository.findAllByProjectIdWithAuthor(projectId);
        List<MemoResponse> memoResponses = memos.stream()
                .map(memo -> MemoMapper.toResponse(memo, memo.getAuthor()))
                .toList();

        return CalItemResponse.builder()
                .year(year)
                .month(month)
                .day(day)
                .events(eventResponses)
                .memos(memoResponses)
                .build();

    }

    // todo 조회 (왼쪽 하단)
    @Transactional(readOnly = true)
    public List<CalTodoResponse> getTodosForCalendar(Long userId, Long projectId) {
        // 권한 체크
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 이벤트를 조회할 수 있습니다.");
        }

        // PrivateTodo 조회
        // 내가 생성한 모든 PrivateTodo를 가져옴
        List<CalTodoResponse> privateTodos = privateTodoRepository.findAllByOwnerId(userId)
                .stream()
                .map(todo -> CalTodoResponse.builder()
                        .id(todo.getId())
                        .type("PRIVATE")
                        .title(todo.getTitle())
                        .description(todo.getDescription())
                        .url(todo.getUrl())
                        .date(todo.getDate())
                        .projectId(todo.getProjectId())
                        .status(todo.getStatus().name())
                        .offsetMinutes(todo.getOffsetMinutes())
                        .build())
                .collect(Collectors.toList());

        // EventTodo 조회
        List<EventTodo> eventTodos = eventTodoRepository.findEventTodosByProjectIdAndUserId(userId, projectId);

        // DTO 변환
        List<CalTodoResponse> eventTodoResponses = eventTodos.stream()
                .map(todo -> CalTodoResponse.builder()
                        .id(todo.getId())
                        .type("EVENT")
                        .title(todo.getTitle())
                        .description(todo.getDescription())
                        .url(todo.getUrl())
                        .eventId(todo.getEvent().getId())          // ManyToOne 매핑 사용
                        .projectId(todo.getEvent().getProject().getId())
                        .status(todo.getStatus().name())
                        .offsetMinutes(todo.getOffsetMinutes())
                        .build())
                .collect(Collectors.toList());

        // 3. 합치기
        List<CalTodoResponse> allTodos = new ArrayList<>();
        allTodos.addAll(privateTodos);
        allTodos.addAll(eventTodoResponses);

        return allTodos;
    }
}
