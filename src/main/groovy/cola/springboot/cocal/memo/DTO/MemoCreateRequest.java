package cola.springboot.cocal.memo.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MemoCreateRequest {

    @NotNull(message = "메모 날짜는 필수입니다.")
    private LocalDate memoDate;   // yyyy-MM-dd

    @NotBlank(message = "내용은 비어 있을 수 없습니다.")
    @Size(max = 20_000, message = "내용은 최대 20,000자까지 가능합니다.")
    private String content;
}