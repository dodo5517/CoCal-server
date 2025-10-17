package cola.springboot.cocal.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByIdAndProjectId(Long id, Long projectId);
    // 특정 프로젝트의 모든 이벤트 조회
    @Query("SELECT e FROM Event e WHERE e.project.id = :projectId")
    List<Event> findAllByProjectId(@Param("projectId") Long projectId);

    Optional<Event> findById(Long id);

    /**
     * 현재 시각(now)을 기준으로 offsetMinutes 만큼 남은 이벤트를 조회
     *
     * 조건:
     * 1. 이벤트 시작 시간(start_at)이 현재 시각부터 1시간 이내인 이벤트
     * 2. 이벤트 시작 시간 - 현재 시각을 분 단위로 계산했을 때
     *    offsetMinutes 이하이면서 offsetMinutes - 1 초과인 이벤트
     *    (즉, offsetMinutes 분 남은 시점에 알림을 보내기 위함)
     *
     * @param now 조회 기준 시각
     * @return offset 기준으로 곧 시작할 이벤트 리스트
     */
    @Query(value = """
    SELECT *
    FROM events e
    WHERE e.start_at BETWEEN CAST(:now AS timestamp) AND (CAST(:now AS timestamp) + INTERVAL '1 hour')
      AND EXTRACT(EPOCH FROM (e.start_at - CAST(:now AS timestamp))) / 60 <= e.offset_minutes
      AND EXTRACT(EPOCH FROM (e.start_at - CAST(:now AS timestamp))) / 60 > e.offset_minutes - 1
    """, nativeQuery = true)
    List<Event> findEventsStartingAfterOffset(@Param("now") LocalDateTime now);

}
