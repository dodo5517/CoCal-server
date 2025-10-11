package cola.springboot.cocal.memo.DTO;

import cola.springboot.cocal.memo.MemoAuthor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class MemoResponse {
    private String id;
    private Long projectId;
    private LocalDate memoDate;
    private String title;
    private String content;
    private String url;
    private List<MemoAuthor> author; // 만든 유저 정보
    private LocalDateTime createdAt;
}