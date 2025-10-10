package cola.springboot.cocal.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByIdAndProjectId(Long id, Long projectId);
    // 특정 프로젝트의 모든 이벤트 조회
    @Query("SELECT e FROM Event e WHERE e.project.id = :projectId")
    List<Event> findAllByProjectId(@Param("projectId") Long projectId);
}
