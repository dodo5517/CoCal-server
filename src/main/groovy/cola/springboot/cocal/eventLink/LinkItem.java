package cola.springboot.cocal.eventLink;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkItem {
    private Long id;
    private String url;
    private Integer orderNo;

    public static LinkItem fromEntity(EventLink link) {
        return LinkItem.builder()
                .id(link.getId())
                .url(link.getUrl())
                .orderNo(link.getOrderNo())
                .build();
    }
}