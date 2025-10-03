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
        INSERT INTO refresh_tokens (user_id, device_info, token_hash, created_at, updated_at, expires_at)
        VALUES (:userId, :deviceInfo, :tokenHash, now(), now(), :expiresAt)
        ON CONFLICT (user_id, device_info) WHERE revoked_at IS NULL
        DO UPDATE SET
            token_hash = EXCLUDED.token_hash,
            updated_at = now()
    """, nativeQuery = true)
    int upsert(@Param("userId") Long userId,
               @Param("deviceInfo") String deviceInfo,
               @Param("tokenHash") byte[] tokenHash,
               @Param("expiresAt") LocalDateTime expiresAt);

    // 해당 기기에서 토큰 폐기
    @Modifying
    @Transactional
    @Query(value = """
    UPDATE refresh_tokens
    SET revoked_at = now(),
        expires_at = now(),
        updated_at = now()
    WHERE user_id = :userId
      AND device_info = :deviceInfo
      AND revoked_at IS NULL
      AND expires_at > now()
    """, nativeQuery = true)
    int logoutDevice(@Param("userId") Long userId,
                     @Param("deviceInfo") String deviceInfo);

    // 해당 유저의 모든 기기 토큰 폐기
    @Modifying
    @Transactional
    @Query(value = """
    UPDATE refresh_tokens
    SET revoked_at = now(),
        expires_at = now(),
        updated_at = now()
    WHERE user_id = :userId
      AND revoked_at IS NULL
      AND expires_at > now()
    """, nativeQuery = true)
    int logoutAllDevices(@Param("userId") Long userId);
}
