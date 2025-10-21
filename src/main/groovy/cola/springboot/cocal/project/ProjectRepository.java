package cola.springboot.cocal.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    // 내가 생성한 프로젝트 or 내가 초대 수락한 프로젝트 조회. 단, 내가 active인 프로잭트만
    @Query("""
        SELECT DISTINCT p
        FROM Project p
        LEFT JOIN Invite i ON i.project = p
        LEFT JOIN ProjectMember pm ON pm.project = p
        WHERE p.owner.id = :userId
           OR (pm.user.id = :userId AND pm.status = 'ACTIVE')
    """)
    Page<Project> findMyProjects(
            @Param("userId") Long userId,
            @Param("email") String email,
            Pageable pageable
    );

}
