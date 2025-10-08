package cola.springboot.cocal.projectMember;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    Boolean existsByProjectIdAndUserIdAndRole(Long projectId, Long userId, ProjectMember.MemberRole role);
    // 특정 프로젝트에 특정 사용자가 멤버인지, 상태까지 ACTIVE인지 확인
    boolean existsByProjectIdAndUserIdAndStatus(Long projectId, Long userId, ProjectMember.MemberStatus status);

    // 멤버 중 status=ACTIVE인 유저 조회
    @Query("""
        select pm
        from ProjectMember pm
        join fetch pm.user u
        where pm.project.id = :projectId
          and pm.status = 'ACTIVE'
        order by u.name asc
    """)
    List<ProjectMember> findActiveMembersWithUser(@Param("projectId") Long projectId);

    // 프로젝트의 멤버 찾기
    @Query("""
      select pm from ProjectMember pm
      where pm.project.id = :projectId and pm.user.id = :userId
    """)
    Optional<ProjectMember> findOne(Long projectId, Long userId);

    // status=ACTIVE인 OWNER 유저 조회
    @Query("""
      select count(pm) from ProjectMember pm
      where pm.project.id = :projectId and pm.role = 'OWNER' and pm.status = 'ACTIVE'
    """)
    long countActiveOwners(Long projectId);

    // 유저Id list가 프로젝트 멤버인지 확인, 상태=ACTIVE
    @Query("""
        select pm.user.id
        from ProjectMember pm
        where pm.project.id = :projectId
          and pm.user.id in :userIds
          and pm.status = 'ACTIVE'
    """)
    Set<Long> findMemberUserIdsInProject(@Param("projectId") Long projectId,
                                         @Param("userIds") Collection<Long> userIds);
}
