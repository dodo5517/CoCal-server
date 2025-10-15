package cola.springboot.cocal.todo.dto;

import cola.springboot.cocal.todo.event_todo.EventTodo;
import cola.springboot.cocal.todo.private_todo.PrivateTodo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드 제외하고 보냄(개인todo는 안 보냄.)
public class TodoItemResponse {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String eventTitle;
    private String eventColor;

    public static TodoItemResponse fromEntity(PrivateTodo t) {
        return TodoItemResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())  
                .status(t.getStatus().name())
                .build();
    }

    public static TodoItemResponse fromEntity(EventTodo t) {
        return TodoItemResponse.builder()
                .id(t.getId())
                .title(t.getTitle()) // 메모 제목
                .eventTitle(t.getEvent().getTitle()) // 종속 이벤트 제목
                .eventColor(t.getEvent().getColor()) // 종속 이벤트 색상
                .description(t.getDescription())
                .status(t.getStatus().name())
                .build();
    }
}
