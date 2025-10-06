package cola.springboot.cocal.event;

import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.event.dto.EventCreateRequest;
import cola.springboot.cocal.event.dto.EventCreateResponse;
import cola.springboot.cocal.event.dto.EventResponse;
import cola.springboot.cocal.invite.InviteRepository;
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

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

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
                .url(request.getUrl()) // null 허용
                .offsetMinutes(request.getOffsetMinutes())
                .color(request.getColor() != null ? request.getColor() : "#0B3559")
                .author(author)
                .build();

        event = eventRepository.save(event);

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
                .url(event.getUrl()) // null이면 그대로 null 반환
                .creatorId(author.getId())
                .createdAt(event.getCreatedAt())
                .offsetMinutes(event.getOffsetMinutes())
                .color(event.getColor())
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

        // DTO로 변환 후 반환
        return EventResponse.fromEntity(event);
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

        return EventResponse.fromEntity(event);

    }
}
