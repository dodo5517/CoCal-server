package cola.springboot.cocal.eventMember;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class EventMemberId implements Serializable {
    private Long eventId;
    private Long userId;
}