package cola.springboot.cocal.project;

import cola.springboot.cocal.invite.dto.MemberListDto.MemberRow;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ProjectResponseDto {
    private Long id;
    private String name;
    private Long ownerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String description;

    private List<ProjectMemberInfoDto> members; // 팀원 최소 정보만 포함
}
