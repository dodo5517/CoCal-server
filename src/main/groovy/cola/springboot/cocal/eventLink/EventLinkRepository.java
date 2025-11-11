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

    List<EventLink> findByEventIdOrderByOrderNoAsc(Long eventId);

    /**
     * 여러 이벤트 ID에 속한 모든 링크를 정렬 순서(order_no) 기준으로 한 번에 조회
     * - PostgreSQL 인덱스(idx_el_event_order) 자동 활용
     */
    @Query("""
        select el
        from EventLink el
        where el.event.id in :eventIds
        order by el.event.id, el.orderNo asc
        """)
    List<EventLink> findAllByEventIds(@Param("eventIds") List<Long> eventIds);
}
