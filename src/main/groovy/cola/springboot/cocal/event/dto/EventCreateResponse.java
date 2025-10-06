package cola.springboot.cocal.event.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCreateResponse {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private boolean allDay;
    private String visibility;
    private String location;
    private String url;
    private Long creatorId;
    private LocalDateTime createdAt;
    private int offsetMinutes;
    private String color;
    private List<Long> memberUserIds;
}
