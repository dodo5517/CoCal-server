package cola.springboot.cocal.todo.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TodoListResponse {
    private Long projectId;
    private LocalDate date;          // 요청 기준 날짜
    private int count;
    private List<TodoItemResponse> items;
}