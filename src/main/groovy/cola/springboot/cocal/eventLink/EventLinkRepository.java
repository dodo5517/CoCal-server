package cola.springboot.cocal.eventLink;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventLinkRepository extends JpaRepository<EventLink, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from EventLink el where el.event.id = :eventId")
    int deleteByEventId(@Param("eventId") Long eventId);
}
