package cola.springboot.cocal.todo;

import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.event.Event;
import cola.springboot.cocal.event.EventRepository;
import cola.springboot.cocal.notification.ReminderService;
import cola.springboot.cocal.project.Project;
import cola.springboot.cocal.project.ProjectRepository;
import cola.springboot.cocal.projectMember.ProjectMember;
import cola.springboot.cocal.projectMember.ProjectMemberRepository;
import cola.springboot.cocal.todo.dto.TodoItemResponse;
import cola.springboot.cocal.todo.dto.TodoListResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PrivateTodoRepository privateTodoRepository;
    private final EventRepository eventRepository;
    private final EventTodoRepository eventTodoRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ReminderService privateReminderService;

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


    // 해당 날짜의 TODO 조회-private
    @Transactional(readOnly = true)
    public TodoListResponse getPrivateDateTodo(Long projectId, Long userId, LocalDate date) {
        // 프로젝트 확인
        projectRepository.findById(projectId)
            .orElseThrow(() -> new BusinessException(
                    HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."
            ));

        // 사용자 확인
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        // 날짜 경계 계산: [start, end)
        LocalDateTime start = date.atStartOfDay();               // 00:00:00
        LocalDateTime end   = date.plusDays(1).atStartOfDay();   // 다음날 00:00:00


        // 조회
        List<PrivateTodo> todos = privateTodoRepository
                .findAllByProjectIdAndOwnerIdAndDateGreaterThanEqualAndDateLessThan(
                        projectId, userId, start, end
                );

        List<TodoItemResponse> items = todos.stream()
                .map(TodoItemResponse::fromEntity)
                .toList();


        return TodoListResponse.builder()
                .projectId(projectId)
                .date(date)
                .count(items.size())
                .items(items)
                .build();
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

    // 해당 날짜의 TODO 조회-event
    @Transactional(readOnly = true)
    public TodoListResponse getEventDateTodo(Long projectId, Long userId, LocalDate date) {
        // 프로젝트 확인
        projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."
                ));

        // 프로젝트 멤버 확인
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 조회할 수 있습니다.");
        }

        // 사용자 확인
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        // 날짜 경계 계산: [start, end)
        LocalDateTime start = date.atStartOfDay();               // 00:00:00
        LocalDateTime end   = date.plusDays(1).atStartOfDay();   // 다음날 00:00:00

        // 조회
        List<EventTodo> todos = eventTodoRepository.findProjectEventTodosOnDate(projectId, start, end);

        List<TodoItemResponse> items = todos.stream()
                .map(TodoItemResponse::fromEntity)
                .toList();;

        return TodoListResponse.builder()
                .projectId(projectId)
                .date(date)
                .count(items.size())
                .items(items)
                .build();
    }

    // 해당 이벤트에 종속된 이벤트 TODO 조회-event
    @Transactional(readOnly = true)
    public TodoListResponse getEventTodoAll(Long projectId, Long userId, Long eventId) {
        // 프로젝트 확인
        projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."
                ));
        
        // 해당 이벤트가 속한 projectId 조회
        Optional<Event> targetEvent = eventRepository.findById(eventId);
        Long targetProjectId = targetEvent.get().getProject().getId();
        
        if (!targetProjectId.equals(projectId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "이 이벤트는 해당 프로젝트에 속하지 않습니다.");
        }

        // 프로젝트 멤버 확인
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 조회할 수 있습니다.");
        }

        // 사용자 확인
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        // 해당 이벤트의 투두 조회
        List<EventTodo> todos = eventTodoRepository.findByEventId(eventId);

        List<TodoItemResponse> items = todos.stream()
                .map(TodoItemResponse::fromEntity)
                .toList();;

        return TodoListResponse.builder()
                .projectId(projectId)
                .count(items.size())
                .items(items)
                .build();
    }

    /*
        TODO 수정 (타입 변경 시 '삭제 후 생성' 방식으로 변경)
    */
    @Transactional
    public TodoResponse updateTodo(Long projectId, Long userId, Long todoId, TodoRequest request) {
        // 1. 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        // 2. 기존 TODO가 어느 타입인지 먼저 확인
        Optional<PrivateTodo> privateTodoOpt = privateTodoRepository.findById(todoId);
        Optional<EventTodo> eventTodoOpt = eventTodoRepository.findById(todoId);

        String originalType;
        if (privateTodoOpt.isPresent()) {
            originalType = "PRIVATE";
        } else if (eventTodoOpt.isPresent()) {
            originalType = "EVENT";
        } else {
            throw new BusinessException(HttpStatus.NOT_FOUND, "TODO_NOT_FOUND", "수정할 TODO를 찾을 수 없습니다.");
        }

        String requestedType = request.getType().toUpperCase();

        // 3. 타입이 변경되었는지 확인
        boolean typeChanged = !originalType.equalsIgnoreCase(requestedType);

        // --- 타입 변경이 없는 경우: 단순 필드 업데이트 ---
        if (!typeChanged) {
            if ("PRIVATE".equalsIgnoreCase(requestedType)) {
                PrivateTodo todo = privateTodoOpt.get();
                // 권한 확인
                if (!todo.getProjectId().equals(projectId) || !todo.getOwnerId().equals(userId)) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "본인 TODO만 수정할 수 있습니다.");
                }

                // [수정] NullPointerException 방지를 위해 Objects.equals 사용
                boolean dateChanged = !java.util.Objects.equals(todo.getDate(), request.getDate());
                boolean offsetChanged = !java.util.Objects.equals(todo.getOffsetMinutes(), request.getOffsetMinutes());
                boolean isTimeChanged = dateChanged || offsetChanged;

                // 값 업데이트
                todo.setTitle(request.getTitle());
                todo.setDescription(request.getDescription());
                todo.setUrl(request.getUrl());
                todo.setDate(request.getDate());
                todo.setStatus(request.getStatus() != null ? PrivateTodo.TodoStatus.valueOf(request.getStatus()) : todo.getStatus());
                todo.setOffsetMinutes(request.getOffsetMinutes());
                todo.setOrderNo(request.getOrderNo());

                PrivateTodo updatedTodo = privateTodoRepository.save(todo);

                if (isTimeChanged) {
                    privateReminderService.handlePrivateTodoTimeChange(updatedTodo);
                }
                return TodoResponse.fromPrivateTodo(updatedTodo);
            } else { // "EVENT"
                EventTodo todo = eventTodoOpt.get();
                // 권한 확인
                boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
                if (!isMember) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 수정할 수 있습니다.");
                }
                if (request.getEventId() == null || !todo.getEventId().equals(request.getEventId())) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "EVENT_ID_MISMATCH", "Event Todo의 소속 이벤트는 변경할 수 없습니다.");
                }

                // 값 업데이트
                todo.setTitle(request.getTitle());
                todo.setDescription(request.getDescription());
                todo.setUrl(request.getUrl());
                todo.setStatus(request.getStatus() != null ? EventTodo.TodoStatus.valueOf(request.getStatus()) : todo.getStatus());
                todo.setOffsetMinutes(request.getOffsetMinutes());
                todo.setOrderNo(request.getOrderNo());

                EventTodo updatedTodo = eventTodoRepository.save(todo);
                return TodoResponse.fromEventTodo(updatedTodo);
            }
        }
        // --- 타입 변경이 있는 경우: 기존 TODO 삭제 후, 새 타입으로 생성 ---
        else {
            // Case 1: Private -> Event 로 변경
            if ("PRIVATE".equalsIgnoreCase(originalType) && "EVENT".equalsIgnoreCase(requestedType)) {
                PrivateTodo oldTodo = privateTodoOpt.get();
                // 권한 확인
                if (!oldTodo.getProjectId().equals(projectId) || !oldTodo.getOwnerId().equals(userId)) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "본인 TODO만 수정할 수 있습니다.");
                }
                if (request.getEventId() == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "EVENT_ID_REQUIRED", "Event 타입으로 변경하려면 이벤트 ID가 필요합니다.");
                }
                // 대상 이벤트 검증
                eventRepository.findById(request.getEventId())
                        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "선택된 이벤트를 찾을 수 없습니다."));

                // 1. 기존 PrivateTodo 삭제
                privateTodoRepository.delete(oldTodo);

                // 2. 새 EventTodo 생성
                EventTodo newTodo = EventTodo.builder()
                        .eventId(request.getEventId())
                        .authorId(userId)
                        .title(request.getTitle())
                        .description(request.getDescription())
                        .url(request.getUrl())
                        .status(request.getStatus() != null ? EventTodo.TodoStatus.valueOf(request.getStatus()) : EventTodo.TodoStatus.IN_PROGRESS)
                        .offsetMinutes(request.getOffsetMinutes())
                        .orderNo(request.getOrderNo())
                        .build();
                EventTodo savedTodo = eventTodoRepository.save(newTodo);
                return TodoResponse.fromEventTodo(savedTodo);
            }
            // Case 2: Event -> Private 로 변경
            else if ("EVENT".equalsIgnoreCase(originalType) && "PRIVATE".equalsIgnoreCase(requestedType)) {
                EventTodo oldTodo = eventTodoOpt.get();
                // 권한 확인
                boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
                if (!isMember) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 수정할 수 있습니다.");
                }
                if (request.getDate() == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "DATE_REQUIRED", "Private 타입으로 변경하려면 날짜가 필요합니다.");
                }

                // 1. 기존 EventTodo 삭제
                eventTodoRepository.delete(oldTodo);

                // 2. 새 PrivateTodo 생성
                PrivateTodo newTodo = PrivateTodo.builder()
                        .projectId(projectId)
                        .ownerId(userId)
                        .title(request.getTitle())
                        .description(request.getDescription())
                        .url(request.getUrl())
                        .date(request.getDate())
                        .status(request.getStatus() != null ? PrivateTodo.TodoStatus.valueOf(request.getStatus()) : PrivateTodo.TodoStatus.IN_PROGRESS)
                        .offsetMinutes(request.getOffsetMinutes())
                        .orderNo(request.getOrderNo())
                        .build();
                PrivateTodo savedTodo = privateTodoRepository.save(newTodo);

                // 새 PrivateTodo에 대한 알림 등록
                privateReminderService.handlePrivateTodoTimeChange(savedTodo);
                return TodoResponse.fromPrivateTodo(savedTodo);
            }
            // 이 외의 경우는 잘못된 요청
            else {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TYPE_CHANGE", "유효하지 않은 타입 변경입니다.");
            }
        }
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
