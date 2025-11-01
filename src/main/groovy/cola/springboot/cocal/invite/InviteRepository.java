package cola.springboot.cocal.invite;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InviteRepository extends JpaRepository<Invite, Long> {
    Optional<Invite> findTopByProject_IdAndEmailIgnoreCaseOrderByCreatedAtDesc(Long projectId, String email);
    Optional<Invite> findByProject_idAndId(Long projectId, Long id);

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

    // 프로젝트의 초대 상태가 PENDING인 초대 요청만 조회
    @Query("""
        select i
        from Invite i
        join fetch i.project p
        left join fetch i.invitedBy inviter
        where p.id = :projectId
          and i.status in :statuses
        order by i.createdAt desc
    """)
    List<Invite> findByProjectAndStatuses(
            @Param("projectId") Long projectId,
            @Param("statuses") Collection<Invite.InviteStatus> statuses
    );

    // 사용자가 해당 프로젝트 초대 수락했는지 확인
    @Query("""
        SELECT COUNT(i) > 0 
        FROM Invite i
        WHERE i.project.id = :projectId
            AND LOWER(i.email) = LOWER(:email)
            AND i.status = 'ACCEPTED' 
    """)
    boolean existsAcceptedInvite(@Param("projectId") Long projectId, @Param("email") String email);

    // 활성 OPEN_LINK 초대 1건 조회(재사용 대상)
    @Query("""
        select i from Invite i
        where i.project.id = :projectId
          and i.type = 'OPEN_LINK'
          and i.status = 'PENDING'
          and (i.expiresAt is null or i.expiresAt > CURRENT_TIMESTAMP)
        order by i.createdAt desc
    """)
    Optional<Invite> findActiveOpenLink(@Param("projectId") Long projectId);

    // 초대 토큰 확인
    @Query("""
        select i
        from Invite i
        join fetch i.project p
        left join fetch i.invitedBy u
        where i.token = :token
        """)
    Optional<Invite> findByTokenWithJoins(String token);
}
