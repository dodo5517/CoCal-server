package cola.springboot.cocal.cal;

import cola.springboot.cocal.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarRepository extends JpaRepository<Event, Long> {

    @Query(value = """
        (
          SELECT DISTINCT EXTRACT(DAY FROM gs::date) AS day
          FROM events e
          JOIN event_todos et ON et.event_id = e.id
          CROSS JOIN generate_series(e.start_at::date, e.end_at::date, interval '1 day') gs
          WHERE e.project_id = :projectId
            AND gs BETWEEN CAST(:monthStart AS date) AND CAST(:monthEnd AS date)
        )
        UNION
        (
          SELECT DISTINCT EXTRACT(DAY FROM t.date) AS day
          FROM private_todos t
          WHERE t.project_id = :projectId
            AND EXTRACT(YEAR FROM t.date) = :year
            AND EXTRACT(MONTH FROM t.date) = :month
        )
        ORDER BY day
        """, nativeQuery = true)
    List<Integer> findActiveDaysByProject(
            @Param("projectId") Long projectId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd,
            @Param("year") int year,
            @Param("month") int month
    );
}