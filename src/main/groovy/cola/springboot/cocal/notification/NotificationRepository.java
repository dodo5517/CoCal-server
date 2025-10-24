package cola.springboot.cocal.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByUserIdAndIsReadFalse(Long userId);

    // 특정 유저, 특정 이벤트, 특정 타입 알림이 이미 존재하는지 체크
    boolean existsByUserIdAndReferenceIdAndType(Long userId, Long referenceId, String type);

    void deleteAllByReferenceIdAndTypeAndIsReadFalse(Long referenceId, String type);

    // 초대 수락/거절 시 사용할 알림id 조회 쿼리
    @Query("SELECT n.id FROM Notification n WHERE n.userId = :userId AND n.referenceId = :referenceId AND n.type = :type")
    Long findIdByUserIdAndReferenceIdAndType(@Param("userId") Long userId,
                                             @Param("referenceId") Long referenceId,
                                             @Param("type") String type);


    // 모든 읽지 않은 알림 읽음 처리
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);
}