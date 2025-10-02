package cola.springboot.cocal.auth;


import cola.springboot.cocal.common.security.JwtTokenProvider;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
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
        // 이메일 존재 유무 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일"));

        // 비밀번호 확인
        if (user.getPassword() == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호 불일치");
        }
        
        // access 토근 생성
        Collection<String> roleStrings = List.of(user.getRole().name());
        String access = jwt.createAccessToken(user.getId(), user.getEmail(), roleStrings);

        // refresh 토큰 hash 생성
        byte[] refreshRaw = randomBytes(32);
        String refreshForClient = base64Url(refreshRaw);
        byte[] refreshHash = sha256(refreshRaw);

        // refresh 토큰 db에 저장
        LocalDateTime now = LocalDateTime.now();
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .tokenHash(refreshHash)
                .createdAt(now)
                .expiresAt(now.plusDays(refreshTtlDays))
                .build();
        refreshTokenRepository.save(rt);

        return new TokenPair(access, refreshForClient);
    }

    // 내부 유틸
    private static byte[] randomBytes(int len) {
        byte[] b = new byte[len];
        new SecureRandom().nextBytes(b);
        return b;
    }
    private static String base64Url(byte[] raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }
    private static byte[] base64UrlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }
    private static byte[] sha256(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 미지원", e);
        }
    }
}