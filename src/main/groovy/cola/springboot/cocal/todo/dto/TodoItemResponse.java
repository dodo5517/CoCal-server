package cola.springboot.cocal.todo.dto;

import cola.springboot.cocal.todo.private_todo.PrivateTodo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TodoItemResponse {
    private Long id;
    private String title;

    public static TodoItemResponse fromEntity(PrivateTodo t) {
        return TodoItemResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .build();
    }
}
