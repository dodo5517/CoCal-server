package cola.springboot.cocal.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodoRequest {
    private String title;
    private String description;
    private String url;
    private LocalDateTime date; // 개인 TODO 용
    private String status;      // 수정 시 필요
    private Integer offsetMinutes;
    private String type;        // "PRIVATE" or "EVENT"
    private Integer orderNo;
    private Long projectId;
    private Long eventId;       // event todo 시에만 사용
}
