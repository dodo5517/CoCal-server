package cola.springboot.cocal.todo.event_todo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventTodoRepository extends JpaRepository<EventTodo, Long> {
    List<EventTodo> findByEventId(Long eventId);

    @Query(value = "SELECT et.* " +
            "FROM event_todos et " +
            "JOIN events e ON et.event_id = e.id " +
            "JOIN project_members pm ON pm.project_id = e.project_id " +
            "WHERE pm.user_id = :userId " +
            "AND pm.status = 'ACTIVE' " +
            "AND e.project_id = :projectId", nativeQuery = true)
    List<EventTodo> findEventTodosByProjectIdAndUserId(@Param("userId") Long userId,
                                                       @Param("projectId") Long projectId);

    @Query("""
        select et
        from EventTodo et
        join fetch et.event e
        where e.project.id = :projectId
          and e.startAt < :end
          and e.endAt >= :start
        order by e.startAt asc, et.orderNo asc, et.id asc
    """)
    List<EventTodo> findProjectEventTodosOnDate(
            @Param("projectId") Long projectId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}
