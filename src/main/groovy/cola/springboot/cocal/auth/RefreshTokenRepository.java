package cola.springboot.cocal.auth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    void deleteByUserId(Long userId);


    // RefreshToken 레코드 저장을 위한 insert or update
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO refresh_tokens (user_id, device_info, token_hash, created_at, expires_at)
        VALUES (:userId, :deviceInfo, :tokenHash, now(), :expiresAt)
        ON CONFLICT (user_id, device_info) DO UPDATE SET
            token_hash = EXCLUDED.token_hash
            -- expires_at은 고정 만료 정책이라 업데이트하지 않음
            -- revoked_at도 건드리지 않음 (revive 금지)
        """, nativeQuery = true)
    int upsert(@Param("userId") Long userId,
               @Param("deviceInfo") String deviceInfo,
               @Param("tokenHash") byte[] tokenHash,
               @Param("expiresAt") LocalDateTime expiresAt);
}
