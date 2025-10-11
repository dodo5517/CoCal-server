package cola.springboot.cocal.cal.DTO;

import cola.springboot.cocal.event.dto.EventResponse;
import cola.springboot.cocal.memo.DTO.MemoResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CalItemResponse {
    private int year;
    private int month;
    private int day;
    private List<EventResponse> events;
    private List<MemoResponse> memos;
}
