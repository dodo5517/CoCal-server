package cola.springboot.cocal.auth;


import cola.springboot.cocal.common.security.JwtTokenProvider;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwt;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom random = new SecureRandom();

    // 운영에서 프로퍼티로 빼고 싶다면 app.auth.refresh-ttl-days 등으로 분리
    private final long refreshTtlDays = 30;

    public record TokenPair(String accessToken, String refreshToken) {}

    // 로그인: 비밀번호 검증 → access 발급 + refresh 저장(해시)
    @Transactional
    public TokenPair login(String email, String rawPassword) {
        // 이메일 존재 유무 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일"));

        // 비밀번호 확인
        if (user.getPassword() == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호 불일치");
        }

        // AccessToken 발급
        Collection<String> roleStrings = List.of(user.getRole().name());
        String access = jwt.createAccessToken(user.getId(), user.getEmail(), roleStrings);

        // RefreshToken 발급
        Pair<String, byte[]> e = jwt.createRefreshToken();
        String refreshForClient = e.getFirst();
        byte[] refreshHash = e.getSecond();

        // refresh 토큰 db에 저장
        refreshTokenService.saveRefreshToken(user, refreshHash);

        return new TokenPair(access, refreshForClient);
    }
}