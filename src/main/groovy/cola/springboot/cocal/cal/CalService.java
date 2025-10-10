package cola.springboot.cocal.cal;

import cola.springboot.cocal.cal.DTO.CalTodoResponse;
import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.projectMember.ProjectMember;
import cola.springboot.cocal.projectMember.ProjectMemberRepository;
import cola.springboot.cocal.todo.event_todo.EventTodo;
import cola.springboot.cocal.todo.event_todo.EventTodoRepository;
import cola.springboot.cocal.todo.private_todo.PrivateTodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalService {
    private final PrivateTodoRepository privateTodoRepository;
    private final EventTodoRepository eventTodoRepository;
    private final ProjectMemberRepository projectMemberRepository;

    // todo 조회
    @Transactional(readOnly = true)
    public List<CalTodoResponse> getTodosForCalendar(Long userId, Long projectId) {
        // 권한 체크
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 이벤트를 조회할 수 있습니다."
            );
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
