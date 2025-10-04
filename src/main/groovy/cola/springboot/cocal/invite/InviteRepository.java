package cola.springboot.cocal.invite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InviteRepository extends JpaRepository<Invite, Long> {
    Optional<Invite> findTopByProject_IdAndEmailIgnoreCaseOrderByCreatedAtDesc(Long projectId, String email);

    // DECLINED 상태의 초대 개수 조회
    @Query (
        """
        SELECT COUNT(i) FROM Invite i WHERE i.project.id = :projectId 
        AND LOWER(i.email) = LOWER(:email) 
        AND i.status = 'DECLINED'
        """
    )
    long countDeclinedInvites(@Param("projectId") Long projectId, @Param("email") String email);
}
