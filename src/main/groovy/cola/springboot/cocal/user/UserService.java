package cola.springboot.cocal.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User signUp(User user) {
        // 이메일 중복 체크
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        user.setCreatedAt(LocalDateTime.now());  // 여기서 생성일 설정
        user.setUpdatedAt(LocalDateTime.now());  // 여기서 수정일 설정

        // 기본 상태와 기본 뷰는 Entity에서 이미 초기화 되어 있음
        return userRepository.save(user);
    }
}
