package cola.springboot.cocal.todo.private_todo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrivateTodoRepository extends JpaRepository<PrivateTodo, Long>{
    List<PrivateTodo> findAllByOwnerId(Long ownerId);
}
