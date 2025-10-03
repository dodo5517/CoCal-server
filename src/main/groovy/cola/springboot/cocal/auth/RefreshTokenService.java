package cola.springboot.cocal.auth;

import cola.springboot.cocal.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    // 운영에서 프로퍼티로 빼고 싶다면 app.auth.refresh-ttl-days 등으로 분리
    private final long refreshTtlDays = 30;
    private final RefreshTokenRepository refreshTokenRepository;

    public void saveRefreshToken(User user, String deviceInfo, byte[] refreshHash) {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(refreshTtlDays);
        refreshTokenRepository.upsert(
                user.getId(),
                deviceInfo,
                refreshHash,
                expiresAt
        );
    }

    public void revokeRefreshToken(Long userId, String deviceInfo) {
        refreshTokenRepository.logoutDevice(userId, deviceInfo);
    }
}
