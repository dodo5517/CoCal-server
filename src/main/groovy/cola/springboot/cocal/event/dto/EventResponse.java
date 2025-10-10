package cola.springboot.cocal.event.dto;

import cola.springboot.cocal.event.Event;
import cola.springboot.cocal.event.dto.EventCreateResponse.LinkItem;
import cola.springboot.cocal.user.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventResponse {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private boolean allDay;
    private String visibility;
    private Long creatorId;
    private String location;
    private String url;
    private int offsetMinutes;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<LinkItem> urls;
    private List<Long> memberUserIds;   // 참가자 ID 목록
    private List<MemberInfo> members;   // 참가자 상세 목록

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MemberInfo {
        private Long id;
        private String name;
        private String email;
        private String profileImageUrl;
    }

    public static EventResponse fromEntity(Event event, List<User> members, List<LinkItem> urls) {
        return EventResponse.builder()
                .id(event.getId())
                .projectId(event.getProject().getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startAt(event.getStartAt())
                .endAt(event.getEndAt())
                .allDay(event.isAllDay())
                .visibility(event.getVisibility().name())
                .location(event.getLocation())
                .creatorId(event.getAuthor().getId())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .offsetMinutes(event.getOffsetMinutes())
                .color(event.getColor())
                .urls(urls)
                // memberUserIds
                .memberUserIds(members.stream().map(User::getId).toList())
                // members 상세
                .members(members.stream()
                        .map(u -> MemberInfo.builder()
                                .id(u.getId())
                                .name(u.getName())
                                .email(u.getEmail())
                                .profileImageUrl(u.getProfileImageUrl())
                                .build())
                        .toList())
                .build();
    }
}
