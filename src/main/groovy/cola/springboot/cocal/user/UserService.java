package cola.springboot.cocal.user;

import cola.springboot.cocal.auth.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder; // Bean 주입
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

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

    @Transactional
    public void changePasswordById(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (user.getProvider() != User.Provider.LOCAL) {
            throw new RuntimeException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public User changeName(Long userId, String newName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        user.setName(newName);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    // 회원 탈퇴
    @Transactional
    public String deleteUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("해당 ID의 유저가 없습니다."));
        // 토큰 삭제 후 유저 삭제
        refreshTokenRepository.deleteByUserId(id);
        userRepository.delete(user);

        // 삭제 완료 메시지
        log.info("Deleted:" + user.getEmail());
        return "탈퇴되었습니다.";
    }
}

