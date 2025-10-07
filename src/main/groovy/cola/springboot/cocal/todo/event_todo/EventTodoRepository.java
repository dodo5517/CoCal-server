package cola.springboot.cocal.todo.event_todo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventTodoRepository extends JpaRepository<EventTodo, Long> {
}
