package cola.springboot.cocal.auth;


import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.common.security.JwtTokenProvider;
import cola.springboot.cocal.common.util.DeviceInfoParser;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwt;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final SecureRandom random = new SecureRandom();

    // 운영에서 프로퍼티로 빼고 싶다면 app.auth.refresh-ttl-days 등으로 분리
    private final long refreshTtlDays = 30;

    public record TokenPair(String accessToken, String refreshToken) {}

    // 로그인: 비밀번호 검증 → access 발급 + refresh 저장(해시)
    @Transactional
    public TokenPair login(String email, String rawPassword, String userAgent) {
        // 이메일 존재 유무 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "EMAIL_NOT_FOUND",
                        "이메일을 찾을 수 없습니다."
                ));


        // 비밀번호 확인
        if (user.getPassword() == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "PASSWORD_MISMATCH",
                    "비밀번호가 일치하지 않습니다."
            );
        }

        // 디바이스 정보 파싱
        String deviceInfo = DeviceInfoParser.extractDeviceInfo(userAgent);
        log.debug("로그인 시도한 deviceInfo: {}", deviceInfo);

        // AccessToken 발급
        Collection<String> roleStrings = List.of(user.getRole().name());
        String access = jwt.createAccessToken(user.getId(), user.getEmail(), roleStrings);

        // RefreshToken 발급
        Pair<String, byte[]> e = jwt.createRefreshToken();
        String refreshForClient = e.getFirst();
        byte[] refreshHash = e.getSecond();

        // refresh 토큰 db에 저장
        refreshTokenService.saveRefreshToken(user, deviceInfo, refreshHash);

        return new TokenPair(access, refreshForClient);
    }

    // 해당 기기에서 로그아웃(device 파싱)
    @Transactional
    public void logoutDevice(Long userId, String userAgent) {
        // 디바이스 정보 파싱
        String deviceInfo = DeviceInfoParser.extractDeviceInfo(userAgent);
        // db에서 refreshToken revoke
        refreshTokenService.revokeRefreshToken(userId,deviceInfo);
    }

    // 토큰 재발급
    @Transactional
    public TokenPair reissueAccessToken(String refreshTokenForClient) {
        // 1. RefreshToken 해시 생성
        byte[] refreshHash = jwt.hashRefreshToken(refreshTokenForClient);

        // 2. DB에서 활성화된 refreshToken 조회
        RefreshToken tokenEntity = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(refreshHash)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_REFRESH_TOKEN",
                        "유효하지 않은 RefreshToken입니다."
                ));

        // 3. 만료 여부 확인
        if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "EXPIRED_REFRESH_TOKEN",
                    "RefreshToken이 만료되었습니다."
            );
        }

        // 4. 새 AccessToken 발급
        User user = tokenEntity.getUser();
        Collection<String> roles = List.of(user.getRole().name());
        String newAccessToken = jwt.createAccessToken(user.getId(), user.getEmail(), roles);

        // 5. 기존 RefreshToken 그대로 반환
        return new TokenPair(newAccessToken, refreshTokenForClient);
    }
}