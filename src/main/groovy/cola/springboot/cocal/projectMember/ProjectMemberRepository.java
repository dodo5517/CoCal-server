package cola.springboot.cocal.projectMember;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    Boolean existsByProjectIdAndUserId(Long projectId, Long userId);
    Boolean existsByProjectIdAndUserIdAndRole(Long projectId, Long userId, ProjectMember.MemberRole role);

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
}
