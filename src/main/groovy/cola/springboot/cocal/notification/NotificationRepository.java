package cola.springboot.cocal.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByUserIdAndIsReadFalse(Long userId);

    // 특정 유저, 특정 이벤트, 특정 타입 알림이 이미 존재하는지 체크
    boolean existsByUserIdAndReferenceIdAndType(Long userId, Long referenceId, String type);

    void deleteAllByReferenceIdAndTypeAndIsReadFalse(Long referenceId, String type);
}