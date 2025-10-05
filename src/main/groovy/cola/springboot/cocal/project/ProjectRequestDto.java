package cola.springboot.cocal.project;

import jakarta.validation.constraints.FutureOrPresent;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProjectRequestDto {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;  // 생략 시 서버에서 기본값 적용
    private String description;
}
