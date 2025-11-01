package cola.springboot.cocal.event.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCreateRequest {
    private Long projectId;
    private String title;
    private String description;
    @NotNull
    private String startDate; // yyyy-MM-dd
    private String startTime; // HH:mm:ss
    @NotNull
    private String endDate;   // yyyy-MM-dd
    @NotNull
    private String endTime;   // HH:mm:ss
    private boolean allDay;
    private String visibility; // PRIVATE / PUBLIC
    private String location;
    private int offsetMinutes;
    private String color;
    private List<String> urls;
    private List<Long> memberUserIds;
}
