package cola.springboot.cocal.cal.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ActiveDaysResponse {
    private List<Integer> activeDays;
}