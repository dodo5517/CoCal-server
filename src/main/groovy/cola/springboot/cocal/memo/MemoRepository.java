package cola.springboot.cocal.memo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo, Long> {
   
    // 해당 프로젝트+날짜의 메모 찾기
    @Query("""
        select m
        from Memo m
        join m.project p
        where p.id = :projectId
          and m.memoDate = :memoDate
        order by m.createdAt desc, m.id desc
    """)
    Page<Memo> findByProjectAndDate(
            @Param("projectId") Long projectId,
            @Param("memoDate") LocalDate memoDate,
            Pageable pageable
    );

    // 프로젝트별 해당 유저 메모 조회
        @Query("""
        select m
        from Memo m
        join m.project p
        left join fetch m.author a
        where m.id = :memoId
          and p.id = :projectId
    """)
        Optional<Memo> findByIdAndProjectIdWithAuthor(
                @Param("memoId") Long memoId,
                @Param("projectId") Long projectId
        );
}
