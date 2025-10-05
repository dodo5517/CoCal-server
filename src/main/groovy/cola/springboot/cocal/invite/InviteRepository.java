package cola.springboot.cocal.invite;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InviteRepository extends JpaRepository<Invite, Long> {
    Optional<Invite> findTopByProject_IdAndEmailIgnoreCaseOrderByCreatedAtDesc(Long projectId, String email);

    // DECLINED 상태의 초대 개수 조회
    @Query(
            """
        SELECT COUNT(i) FROM Invite i WHERE i.project.id = :projectId 
        AND LOWER(i.email) = LOWER(:email) 
        AND i.status = 'DECLINED'
        """
    )
    long countDeclinedInvites(@Param("projectId") Long projectId, @Param("email") String email);

    // 내가 받은 초대 목록 (email + optional status)
    @Query("""
        select i
        from Invite i
        join fetch i.project p
        left join fetch i.invitedBy u
        where lower(i.email) = lower(:email)
          and (:status is null or i.status = :status)
        """)
    Page<Invite> findMyInvitesWithJoins(
            @Param("email") String email,
            @Param("status") Invite.InviteStatus status,
            Pageable pageable
    );


}
