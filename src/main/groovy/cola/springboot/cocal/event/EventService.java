package cola.springboot.cocal.event;

import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.event.dto.EventCreateRequest;
import cola.springboot.cocal.event.dto.EventCreateResponse;
import cola.springboot.cocal.event.dto.EventResponse;
import cola.springboot.cocal.eventLink.EventLink;
import cola.springboot.cocal.eventLink.EventLinkRepository;
import cola.springboot.cocal.eventMember.EventMember;
import cola.springboot.cocal.eventMember.EventMemberRepository;
import cola.springboot.cocal.project.Project;
import cola.springboot.cocal.project.ProjectRepository;
import cola.springboot.cocal.projectMember.ProjectMember;
import cola.springboot.cocal.projectMember.ProjectMemberRepository;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final EventLinkRepository eventLinkRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final EventMemberRepository eventMemberRepository;

    // event(일정) 생성
    @Transactional
    public EventCreateResponse createEvent(Long projectId, Long userId, String email, EventCreateRequest request) {
        // 프로젝트 확인
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."
                ));

        // 프로젝트 멤버 여부 확인 (OWNER 또는 MEMBER 상태가 ACTIVE)
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 이벤트를 생성할 수 있습니다."
            );
        }

        // 사용자 확인
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."
                ));

        // 날짜 / 시간 병합
        LocalDateTime startAt = LocalDateTime.of(
                LocalDate.parse(request.getStartDate()),
                LocalTime.parse(request.getStartTime())
        );

        LocalDateTime endAt = LocalDateTime.of(
                LocalDate.parse(request.getEndDate()),
                LocalTime.parse(request.getEndTime())
        );

        // visibility 변환
        Event.Visibility visibility = Event.Visibility.valueOf(request.getVisibility());

        Event event = Event.builder()
                .project(project)
                .title(request.getTitle())
                .description(request.getDescription())
                .startAt(startAt)
                .endAt(endAt)
                .allDay(request.isAllDay())
                .visibility(visibility)
                .location(request.getLocation())
                .offsetMinutes(request.getOffsetMinutes())
                .color(request.getColor() != null ? request.getColor() : "#0B3559")
                .author(author)
                .build();

        event = eventRepository.save(event);

        // URL 처리
        List<String> urls = Optional.ofNullable(request.getUrls()).orElseGet(List::of).stream()
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isEmpty())
                .distinct() // 중복 제거 원치 않으면 제거
                .toList();

        List<EventLink> links = new ArrayList<>(urls.size());
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            // 길이/스킴 검증
            if (url.length() > 2000) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "URL_TOO_LONG", "URL 길이가 너무 깁니다.");
            }
            if (!(url.startsWith("http://") || url.startsWith("https://"))) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "URL_SCHEME", "http/https만 허용합니다.");
            }
            // EventLink 생성
            EventLink link = EventLink.builder()
                    .event(event)
                    .url(url)
                    .orderNo(i)
                    .createdBy(author)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            links.add(link);
        }
        // 저장 (비어있지 않다면)
        if (!links.isEmpty()) {
            eventLinkRepository.saveAll(links);
        }

        // url 응답 생성
        List<EventCreateResponse.LinkItem> linkItems = links.stream()
                .map(l -> EventCreateResponse.LinkItem.builder()
                        .id(l.getId())
                        .url(l.getUrl())
                        .orderNo(l.getOrderNo())
                        .build())
                .toList();

        // 이벤트 멤버 세트 구성
        Set<Long> userIds = new HashSet<>();
        if (request.getMemberUserIds() != null) {
            request.getMemberUserIds().stream()
                    .filter(Objects::nonNull)
                    .forEach(userIds::add);
        }
        userIds.add(userId); // 본인은 자동 포함

        // 프로젝트 멤버 검증
        Set<Long> projectMemberIds = projectMemberRepository
                .findMemberUserIdsInProject(projectId, userIds);
        if (projectMemberIds.size() != userIds.size()) {
            // 프로젝트 멤버가 아닌 userId들 식별
            Set<Long> notMembers = new HashSet<>(userIds);
            // 요청 들어온 Ids에서 검증된 프로젝트 멤버 Ids 제거하여 멤버 아닌 사용자 식별
            notMembers.removeAll(projectMemberIds);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "NOT_PROJECT_MEMBER",
                    "프로젝트 멤버가 아닌 사용자 포함: " + notMembers);
        }

        // EventMember 일괄 저장
        Event finalEvent = event;
        List<EventMember> savedMembers = eventMemberRepository.saveAll(
                projectMemberIds.stream()
                        .map(uid -> EventMember.of(finalEvent, userRepository.getReferenceById(uid)))
                        .toList()
        );

        List<Long> memberUserIds = savedMembers.stream()
                .map(em -> em.getUser().getId())
                .toList();

        return EventCreateResponse.builder()
                .id(event.getId())
                .projectId(project.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startAt(event.getStartAt())
                .endAt(event.getEndAt())
                .allDay(event.isAllDay())
                .visibility(event.getVisibility().name())
                .location(event.getLocation())
                .urls(linkItems) // null이면 그대로 null 반환
                .creatorId(author.getId())
                .createdAt(event.getCreatedAt())
                .offsetMinutes(event.getOffsetMinutes())
                .color(event.getColor())
                .memberUserIds(memberUserIds)
                .build();
    }

    // 이벤트(개별) 조회
    @Transactional(readOnly = true)
    public EventResponse getEvent(Long id, Long projectId, Long userId) {
        // 프로젝트 확인
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."
                ));

        // 이벤트 확인
        Event event =  eventRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "이벤트를 찾을 수 없습니다."
                ));

        // 프로젝트 멤버 여부 확인 (OWNER 또는 MEMBER 상태가 ACTIVE)
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 이벤트를 조회할 수 있습니다."
            );
        }

        // 이벤트 참가자 조회
        List<User> eventMembers = eventMemberRepository.findUsersByEventId(id);

        // EventResponse에 members와 memberUserIds 둘 다 포함
        return EventResponse.fromEntity(event, eventMembers);
    }

    // 이벤트 수정
    @Transactional
    public EventResponse updateEvent(Long id, Long projectId, EventCreateRequest request, Long userId) {
        Event event = eventRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "이벤트를 찾을 수 없습니다."
                ));

        // 프로젝트 멤버 여부 확인 (OWNER 또는 MEMBER 상태가 ACTIVE)
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 이벤트를 수정할 수 있습니다."
            );
        }

        // 날짜 / 시간 병합
        LocalDateTime startAt = LocalDateTime.of(
                LocalDate.parse(request.getStartDate()),
                LocalTime.parse(request.getStartTime())
        );

        LocalDateTime endAt = LocalDateTime.of(
                LocalDate.parse(request.getEndDate()),
                LocalTime.parse(request.getEndTime())
        );

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setStartAt(startAt);
        event.setEndAt(endAt);
        event.setAllDay(request.isAllDay());
        event.setVisibility(Event.Visibility.valueOf(request.getVisibility()));
        event.setLocation(request.getLocation());
        event.setUrl(request.getUrl());
        event.setOffsetMinutes(request.getOffsetMinutes());
        event.setColor(request.getColor());
        event.setUpdatedAt(LocalDateTime.now());

        // DB 반영
        event = eventRepository.save(event);

        // 이벤트 참가자 조회
        List<User> eventMembers = eventMemberRepository.findUsersByEventId(id);

        return EventResponse.fromEntity(event, eventMembers);
    }

    // 이벤트 삭제
    @Transactional
    public void deleteEvent(Long id, Long projectId, Long userId) {
        // 프로젝트 확인
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없습니다."
                ));

        // 이벤트 확인
        Event event =  eventRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "이벤트를 찾을 수 없습니다."
                ));

        // 프로젝트 멤버 여부 확인 (OWNER 또는 MEMBER 상태가 ACTIVE)
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMember.MemberStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "FORBIDDEN", "프로젝트 멤버만 이벤트를 삭제할 수 있습니다."
            );
        }

        // 이벤트 삭제
        eventRepository.delete(event);
    }
}
