package cola.springboot.cocal.cal;

import cola.springboot.cocal.cal.DTO.ActiveDaysResponse;
import cola.springboot.cocal.cal.DTO.CalItemResponse;
import cola.springboot.cocal.cal.DTO.CalTodoResponse;
import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.event.Event;
import cola.springboot.cocal.event.EventRepository;
import cola.springboot.cocal.event.dto.EventResponse;
import cola.springboot.cocal.eventLink.EventLink;
import cola.springboot.cocal.eventLink.LinkItem;
import cola.springboot.cocal.eventLink.EventLinkRepository;
import cola.springboot.cocal.eventMember.EventMember;
import cola.springboot.cocal.eventMember.EventMemberRepository;
import cola.springboot.cocal.invite.InviteRepository;
import cola.springboot.cocal.memo.DTO.MemoMapper;
import cola.springboot.cocal.memo.DTO.MemoResponse;
import cola.springboot.cocal.memo.MemoRepository;
import cola.springboot.cocal.project.Project;
import cola.springboot.cocal.project.ProjectRepository;
import cola.springboot.cocal.projectMember.ProjectMember;
import cola.springboot.cocal.projectMember.ProjectMemberRepository;
import cola.springboot.cocal.todo.event_todo.EventTodo;
import cola.springboot.cocal.todo.event_todo.EventTodoRepository;
import cola.springboot.cocal.todo.private_todo.PrivateTodoRepository;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalService {
    private final PrivateTodoRepository privateTodoRepository;
    private final EventTodoRepository eventTodoRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final EventRepository eventRepository;
    private final EventLinkRepository eventLinkRepository;
    private final EventMemberRepository eventMemberRepository;
    private final MemoRepository memoRepository;
    private final CalendarRepository calendarRepository;
    private final UserRepository userRepository;
    private final InviteRepository inviteRepository;
    private static final Logger log = LoggerFactory.getLogger(CalService.class);

    // event, memo 조회(calendar 화면에서)
    @Transactional(readOnly = true)
    public CalItemResponse getCalendarItems(Long userId, Long projectId) {
        //  프로젝트의 시작일 가져오기
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."));

        LocalDate startDate = project.getStartDate(); // Project 엔티티에 startDate 필드

        // 권한 체크
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 조회할 수 있습니다.");
        }

        // 이벤트 조회
        List<Event> events = eventRepository.findAllByProjectId(projectId);
        if (events.isEmpty()) {
            return CalItemResponse.builder()
                    .year(startDate.getYear())
                    .month(startDate.getMonthValue())
                    .day(startDate.getDayOfMonth())
                    .events(List.of())
                    .memos(List.of())
                    .build();
        }

        // 이벤트 ID 목록 추출
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        // 멤버 + 링크 한 번에 조회
        List<EventMember> members = eventMemberRepository.findAllByEventIds(eventIds);
        List<EventLink> links = eventLinkRepository.findAllByEventIds(eventIds);

        // 6eventId 기준으로 그룹핑 (메모리상 매핑)
        Map<Long, List<User>> memberMap = members.stream()
                .collect(Collectors.groupingBy(
                        em -> em.getEvent().getId(),
                        Collectors.mapping(EventMember::getUser, Collectors.toList())
                ));

        Map<Long, List<LinkItem>> linkMap = links.stream()
                .collect(Collectors.groupingBy(
                        el -> el.getEvent().getId(),
                        Collectors.mapping(LinkItem::fromEntity, Collectors.toList())
                ));

        // EventResponse 변환
        List<EventResponse> eventResponses = events.stream()
                .map(event -> EventResponse.fromEntity(
                        event,
                        memberMap.getOrDefault(event.getId(), List.of()),
                        linkMap.getOrDefault(event.getId(), List.of())
                ))
                .toList();

        // 메모 조회
        List<MemoResponse> memoResponses = memoRepository.findAllByProjectIdWithAuthor(projectId)
                .stream()
                .map(memo -> MemoMapper.toResponse(memo, memo.getAuthor()))
                .toList();

        // 최종 응답 조립
        return CalItemResponse.builder()
                .year(startDate.getYear())
                .month(startDate.getMonthValue())
                .day(startDate.getDayOfMonth())
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


    // 개인/이벤트 todo가 있는 날짜 조회
    public ActiveDaysResponse getActiveDays(Long userId, Long projectId, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "사용자를 찾을 수 없습니다."
                ));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "PROJECT_NOT_FOUND",
                        "존재하지 않는 프로젝트입니다."
                ));

        // 소유자 이거나 초대 받은 사용자 중 수락한 경우만
        boolean isOwner = project.getOwner().getId().equals(userId);
        boolean isAcceptedInvitee = inviteRepository.existsAcceptedInvite(projectId, user.getEmail());

        if (!isOwner && !isAcceptedInvitee) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "프로젝트 접근 권한이 없습니다."
            );
        }

        ZoneId kst = ZoneId.of("Asia/Seoul");
        ZoneId utc = ZoneId.of("UTC");

        // KST 기준 월의 시작
        LocalDateTime kstStart = LocalDate.of(year, month, 1).atStartOfDay();

        // KST 기준 월의 끝: 다음달 1일 00:00
        LocalDateTime kstEnd = LocalDate.of(year, month, 1)
                .plusMonths(1)
                .atStartOfDay();

        // UTC 변환
        LocalDateTime utcStart = kstStart.atZone(kst).withZoneSameInstant(utc).toLocalDateTime();

        //  monthEnd에서 1ns라도 더 줄이기 (exclusive 비교를 보장)
        LocalDateTime utcEnd = kstEnd.atZone(kst)
                .withZoneSameInstant(utc)
                .toLocalDateTime()
                .minusNanos(1);

        List<Integer> days = calendarRepository.findActiveDaysByProject(
                projectId,
                utcStart,
                utcEnd,
                year,
                month
        );

        return new ActiveDaysResponse(days);
    }
}
