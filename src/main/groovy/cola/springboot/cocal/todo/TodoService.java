package cola.springboot.cocal.todo;

import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.event.Event;
import cola.springboot.cocal.event.EventRepository;
import cola.springboot.cocal.project.Project;
import cola.springboot.cocal.project.ProjectRepository;
import cola.springboot.cocal.projectMember.ProjectMember;
import cola.springboot.cocal.projectMember.ProjectMemberRepository;
import cola.springboot.cocal.todo.dto.TodoRequest;
import cola.springboot.cocal.todo.dto.TodoResponse;
import cola.springboot.cocal.todo.event_todo.EventTodo;
import cola.springboot.cocal.todo.event_todo.EventTodoRepository;
import cola.springboot.cocal.todo.private_todo.PrivateTodo;
import cola.springboot.cocal.todo.private_todo.PrivateTodoRepository;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PrivateTodoRepository privateTodoRepository;
    private final EventRepository eventRepository;
    private final EventTodoRepository eventTodoRepository;
    private final ProjectMemberRepository projectMemberRepository;

    // TODO 생성
    @Transactional
    public TodoResponse createTodo(Long projectId, Long userId, TodoRequest request) {
        // 1. 사용자 확인
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."
                ));

        // 1-2. 프로젝트 멤버 여부 확인 (OWNER 또는 MEMBER 상태가 ACTIVE)
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 todo를 생성할 수 있습니다."
            );
        }

        // 2. PRIVATE TODO 처리
        if ("PRIVATE".equalsIgnoreCase(request.getType())) {
            // 프로젝트 확인
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."
                    ));

            PrivateTodo todo = PrivateTodo.builder()
                    .projectId(project.getId())
                    .ownerId(author.getId())
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .url(request.getUrl())
                    .date(request.getDate())
                    .status(request.getStatus() != null
                            ? PrivateTodo.TodoStatus.valueOf(request.getStatus())
                            : PrivateTodo.TodoStatus.IN_PROGRESS)
                    .offsetMinutes(request.getOffsetMinutes() != null ? request.getOffsetMinutes() : 0)
                    .orderNo(request.getOrderNo() != null ? request.getOrderNo() : 0)
                    .build();

            todo = privateTodoRepository.save(todo);
            return TodoResponse.fromPrivateTodo(todo);

        }
        // 3. EVENT TODO 처리
        else if ("EVENT".equalsIgnoreCase(request.getType())) {
            if (request.getEventId() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "EVENT_ID_REQUIRED", "이벤트 ID가 필요합니다.");
            }

            Event event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "이벤트를 찾을 수 없습니다."
                    ));

            EventTodo todo = EventTodo.builder()
                    .eventId(event.getId())
                    .authorId(author.getId())
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .url(request.getUrl())
                    .status(request.getStatus() != null
                            ? EventTodo.TodoStatus.valueOf(request.getStatus())
                            : EventTodo.TodoStatus.IN_PROGRESS)
                    .offsetMinutes(request.getOffsetMinutes() != null ? request.getOffsetMinutes() : 0)
                    .orderNo(request.getOrderNo() != null ? request.getOrderNo() : 0)
                    .build();

            todo = eventTodoRepository.save(todo);
            return TodoResponse.fromEventTodo(todo);
        }

        throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TYPE", "유효하지 않은 TODO 타입입니다. PRIVATE 또는 EVENT만 가능합니다.");
    }

}
