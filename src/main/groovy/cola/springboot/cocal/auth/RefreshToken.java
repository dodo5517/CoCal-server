package cola.springboot.cocal.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "auth_refresh_tokens",
        indexes = {
                @Index(name = "idx_user_active", columnList = "user_id, expires_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_token_hash", columnNames = "token_hash")
        })
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // users(id) FK
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // SHA-256(token) 32바이트
    @Column(name = "token_hash", nullable = false, columnDefinition = "BINARY(32)")
    private byte[] tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false,
            columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;


    // 사용중인 상태인지 확인하는 메서드
    public boolean isActive(LocalDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
    
    // 폐기된 시간을 저장하는 메서드
    public void revoke(LocalDateTime now) {
        this.revokedAt = now;
    }
}