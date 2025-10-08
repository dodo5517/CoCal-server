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

import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PrivateTodoRepository privateTodoRepository;
    private final EventRepository eventRepository;
    private final EventTodoRepository eventTodoRepository;
    private final ProjectMemberRepository projectMemberRepository;

    /*
        TODO 생성
    */
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

    /*
        TODO 조회-private
    */
    @Transactional(readOnly = true)
    public TodoResponse getPrivateTodo(Long projectId, Long userId, Long todoId) {
        // 사용자 확인
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        // TODO 찾기
        PrivateTodo todo = privateTodoRepository.findById(todoId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TODO_NOT_FOUND", "TODO를 찾을 수 없습니다."));

        // 접근 권한 확인
        if (!todo.getProjectId().equals(projectId) || !todo.getOwnerId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "본인 TODO만 조회할 수 있습니다.");
        }

        return TodoResponse.fromPrivateTodo(todo);
    }

    /*
        TODO 조회-event
    */
    @Transactional(readOnly = true)
    public TodoResponse getEventTodo(Long projectId, Long userId, Long eventId, Long todoId) {
        // 프로젝트 멤버 확인
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 조회할 수 있습니다.");
        }

        // 이벤트 TODO 조회
        EventTodo todo = eventTodoRepository.findById(todoId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TODO_NOT_FOUND", "TODO를 찾을 수 없습니다."));

        // 이벤트와 프로젝트 일치 여부 확인
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "이벤트를 찾을 수 없습니다."));
        if (!event.getProject().getId().equals(projectId) || !todo.getEventId().equals(eventId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_RELATION", "이 TODO는 해당 이벤트에 속하지 않습니다.");
        }

        return TodoResponse.fromEventTodo(todo);
    }

    /*
        TODO 수정
    */
    @Transactional
    public TodoResponse updateTodo(Long projectId, Long userId, Long todoId, TodoRequest request) {
        // 1. 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        // 2. TODO 타입 분기
        if ("PRIVATE".equalsIgnoreCase(request.getType())) {
            // Private TODO 조회
            PrivateTodo todo = privateTodoRepository.findById(todoId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TODO_NOT_FOUND", "TODO를 찾을 수 없습니다."));

            // 접근 권한 확인 (본인만 수정 가능)
            if (!todo.getProjectId().equals(projectId) || !todo.getOwnerId().equals(userId)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "본인 TODO만 수정할 수 있습니다.");
            }

            // 값 업데이트
            todo.setTitle(request.getTitle());
            todo.setDescription(request.getDescription());
            todo.setUrl(request.getUrl());
            todo.setDate(request.getDate());
            todo.setStatus(request.getStatus() != null ? PrivateTodo.TodoStatus.valueOf(request.getStatus()) : PrivateTodo.TodoStatus.IN_PROGRESS);
            todo.setOffsetMinutes(request.getOffsetMinutes() != null ? request.getOffsetMinutes() : 0);
            todo.setOrderNo(request.getOrderNo() != null ? request.getOrderNo() : 0);

            todo = privateTodoRepository.save(todo);
            return TodoResponse.fromPrivateTodo(todo);
        }
        else if ("EVENT".equalsIgnoreCase(request.getType())) {
            if (request.getEventId() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "EVENT_ID_REQUIRED", "이벤트 ID가 필요합니다.");
            }

            // Event TODO 조회
            EventTodo todo = eventTodoRepository.findById(todoId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TODO_NOT_FOUND", "TODO를 찾을 수 없습니다."));

            // 프로젝트 멤버 확인
            boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
            if (!isMember) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 수정할 수 있습니다.");
            }

            // 이벤트와 프로젝트 일치 확인
            Event event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "이벤트를 찾을 수 없습니다."));
            if (!event.getProject().getId().equals(projectId) || !todo.getEventId().equals(request.getEventId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_RELATION", "이 TODO는 해당 이벤트에 속하지 않습니다.");
            }

            // 값 업데이트
            todo.setTitle(request.getTitle());
            todo.setDescription(request.getDescription());
            todo.setUrl(request.getUrl());
            todo.setStatus(request.getStatus() != null ? EventTodo.TodoStatus.valueOf(request.getStatus()) : EventTodo.TodoStatus.IN_PROGRESS);
            todo.setOffsetMinutes(request.getOffsetMinutes() != null ? request.getOffsetMinutes() : 0);
            todo.setOrderNo(request.getOrderNo() != null ? request.getOrderNo() : 0);

            todo = eventTodoRepository.save(todo);
            return TodoResponse.fromEventTodo(todo);
        }

        throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TYPE", "유효하지 않은 TODO 타입입니다. PRIVATE 또는 EVENT만 가능합니다.");
    }

    /*
        TODO 삭제
    */
    @Transactional
    public void deleteTodo(Long projectId, Long userId, Long todoId, String type, Long eventId) {
        if ("PRIVATE".equalsIgnoreCase(type)) {
            // Private TODO 조회
            PrivateTodo todo = privateTodoRepository.findById(todoId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TODO_NOT_FOUND", "TODO를 찾을 수 없습니다."));

            // 접근 권한 확인 (본인만 삭제 가능)
            if (!todo.getProjectId().equals(projectId) || !todo.getOwnerId().equals(userId)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "본인 TODO만 삭제할 수 있습니다.");
            }

            privateTodoRepository.delete(todo);
        }
        else if ("EVENT".equalsIgnoreCase(type)) {
            if (eventId == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "EVENT_ID_REQUIRED", "이벤트 ID가 필요합니다.");
            }

            // Event TODO 조회
            EventTodo todo = eventTodoRepository.findById(todoId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TODO_NOT_FOUND", "TODO를 찾을 수 없습니다."));

            // 프로젝트 멤버 확인
            boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
            if (!isMember) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 삭제할 수 있습니다.");
            }

            // 이벤트와 프로젝트 일치 확인
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "이벤트를 찾을 수 없습니다."));
            if (!event.getProject().getId().equals(projectId) || !todo.getEventId().equals(eventId)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_RELATION", "이 TODO는 해당 이벤트에 속하지 않습니다.");
            }

            eventTodoRepository.delete(todo);
        }
        else {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TYPE", "유효하지 않은 TODO 타입입니다. PRIVATE 또는 EVENT만 가능합니다.");
        }
    }
}
