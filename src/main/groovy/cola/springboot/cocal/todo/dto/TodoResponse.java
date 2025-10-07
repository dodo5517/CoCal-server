package cola.springboot.cocal.todo.dto;

import cola.springboot.cocal.todo.event_todo.EventTodo;
import cola.springboot.cocal.todo.private_todo.PrivateTodo;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoResponse {
    private Long id;
    private String title;
    private String description;
    private String url;
    private LocalDateTime date; // 개인 TODO 용
    private String status;      // 수정 시 필요
    private Integer offsetMinutes;
    private String type;        // "PRIVATE" or "EVENT"
    private Integer orderNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long authorId;
    private Long projectId;
    private Long eventId;
    private Long userId;

    public static TodoResponse fromPrivateTodo(PrivateTodo todo) {
        return TodoResponse.builder()
                .id(todo.getId())
                .title(todo.getTitle())
                .description(todo.getDescription())
                .url(todo.getUrl())
                .date(todo.getDate())
                .status(todo.getStatus().name())
                .offsetMinutes(todo.getOffsetMinutes())
                .type("PRIVATE")
                .orderNo(todo.getOrderNo())
                .createdAt(todo.getCreatedAt())
                .updatedAt(todo.getUpdatedAt())
                .projectId(todo.getProjectId())
                .userId(todo.getOwnerId())
                .build();
    }

    public static TodoResponse fromEventTodo(EventTodo todo) {
        return TodoResponse.builder()
                .id(todo.getId())
                .title(todo.getTitle())
                .description(todo.getDescription())
                .url(todo.getUrl())
                .date(null) // 이벤트 TODO는 date 없으므로 null
                .status(todo.getStatus().name())
                .offsetMinutes(todo.getOffsetMinutes())
                .type("EVENT")
                .orderNo(todo.getOrderNo())
                .createdAt(todo.getCreatedAt())
                .updatedAt(todo.getUpdatedAt())
                .authorId(todo.getAuthorId())
                .eventId(todo.getEventId())
                .userId(todo.getAuthorId())
                .build();
    }
}
