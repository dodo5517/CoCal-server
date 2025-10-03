package cola.springboot.cocal.auth;

import cola.springboot.cocal.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_user_active", columnList = "user_id, expires_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_token_hash", columnNames = "token_hash"),
                @UniqueConstraint(columnNames = {"user_id", "device_info"})
        })
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // users(id) FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refresh_token_user"))
    private User user;

    @Column(name = "device_info", length = 255)
    private String deviceInfo; // 로그인한 장치 ex. PC, 모바일, 태블릿

    // SHA-256(token) 32바이트
    @Column(name = "token_hash", nullable = false)
    private byte[] tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

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