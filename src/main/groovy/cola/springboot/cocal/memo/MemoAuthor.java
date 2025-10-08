package cola.springboot.cocal.memo;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoAuthor {
    private Long userId;
    private String name;
    private String email;
    private String profileImageUrl; // null 허용
}