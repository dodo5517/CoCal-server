package cola.springboot.cocal.memo.DTO;

import cola.springboot.cocal.memo.Memo;
import cola.springboot.cocal.memo.MemoAuthor;
import cola.springboot.cocal.user.User;

import java.util.List;

public class MemoMapper {
    public static MemoResponse toResponse(Memo m, User user) {
        MemoAuthor author = MemoAuthor.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .build();

        return MemoResponse.builder()
                .id(String.valueOf(m.getId()))
                .projectId(m.getProject().getId())
                .memoDate(m.getMemoDate())
                .title(m.getTitle())
                .content(m.getContent())
                .url(m.getUrl())
                .author(List.of(author)) // 단일 작성자라도 리스트 형태로 감싸줌
                .createdAt(m.getCreatedAt())
                .build();
    }
}
