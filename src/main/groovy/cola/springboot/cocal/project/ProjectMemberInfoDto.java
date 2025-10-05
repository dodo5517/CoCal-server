package cola.springboot.cocal.project;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberInfoDto {
    private Long userId;
    private String name;
    private String email;
    private String profileImageUrl; // null 허용
}
