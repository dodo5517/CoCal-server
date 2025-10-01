package cola.springboot.cocal.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Bean 주입

    @Transactional
    public User signUp(User user) {
        // 이메일 중복 체크
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        // 비밀번호 null 체크
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new RuntimeException("비밀번호를 입력해주세요.");
        }

        // 원본 비밀번호 로그
        System.out.println("원본 비밀번호: " + user.getPassword());

        // BCrypt 해시
        String hashed = passwordEncoder.encode(user.getPassword());
        System.out.println("BCrypt 해시: " + hashed);
        user.setPassword(hashed);

        // 생성일, 수정일
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // DB 저장
        return userRepository.save(user); // password는 DB에 저장, 응답에는 숨김(JsonProperty) 처리
    }
}

