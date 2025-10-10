package cola.springboot.cocal.cal.DTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class CalTodoResponse {
    private Long id;
    private String type;    // private or event
    private String title;
    private String description;
    private String url;
    private LocalDateTime date; // private-todo 날짜
    private Long eventId;   // event-todo 일 경우 이벤트 ID
    private Long projectId; // 소속 프로젝트
    private String status;
    private Integer offsetMinutes;
}
