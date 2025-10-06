package cola.springboot.cocal.event.dto;

import cola.springboot.cocal.event.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    public static EventResponse fromEntity(Event event) {
        return new EventResponse(
                event.getId(),
                event.getProject().getId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartAt(),
                event.getEndAt(),
                event.isAllDay(),
                event.getVisibility().name(),
                event.getAuthor().getId(),
                event.getLocation(),
                event.getUrl(),
                event.getOffsetMinutes(),
                event.getColor(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
