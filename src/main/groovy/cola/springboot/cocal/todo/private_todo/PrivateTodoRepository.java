package cola.springboot.cocal.todo.private_todo;

import cola.springboot.cocal.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PrivateTodoRepository extends JpaRepository<PrivateTodo, Long>{
    List<PrivateTodo> findAllByOwnerId(Long ownerId);

    /**
     * 현재 시각(now)을 기준으로 offsetMinutes 만큼 남은 개인 TODO를 조회
     *
     * 조건:
     * 1. TODO 날짜(date)가 NULL이 아닌 경우
     * 2. TODO 날짜(date)가 현재 시각부터 1시간 이내인 경우
     * 3. TODO 시작 시간 - 현재 시각을 분 단위로 계산했을 때
     *    offset_minutes 이하이면서 offset_minutes - 1 초과인 경우
     *    (즉, offset_minutes 분 남은 시점에 알림을 보내기 위함)
     *
     * @param now 조회 기준 시각
     * @return offset 기준으로 곧 시작할 개인 TODO 리스트
     */
    @Query(value = """
    SELECT *
    FROM private_todos p
    WHERE p.date IS NOT NULL
      AND p.date BETWEEN CAST(:now AS timestamp) AND (CAST(:now AS timestamp) + INTERVAL '1 hour')
      AND EXTRACT(EPOCH FROM (p.date - CAST(:now AS timestamp))) / 60 <= p.offset_minutes
      AND EXTRACT(EPOCH FROM (p.date - CAST(:now AS timestamp))) / 60 > p.offset_minutes - 1
    """, nativeQuery = true)
    List<PrivateTodo> findTodosStartingAfterOffset(@Param("now") LocalDateTime now);
}
