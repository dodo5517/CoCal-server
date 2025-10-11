package cola.springboot.cocal.memo;

import cola.springboot.cocal.project.Project;
import cola.springboot.cocal.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "memos",
        indexes = {
                @Index(name = "idx_memos_project_date", columnList = "project_id, memo_date")
        }
)
public class Memo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "project_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_memos_project")
    )
    private Project project;

    @Column(name = "memo_date", nullable = false)
    private LocalDate memoDate;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "url", length = 2000)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "author_id",
            foreignKey = @ForeignKey(name = "fk_memos_author")
    )
    private User author;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;
}