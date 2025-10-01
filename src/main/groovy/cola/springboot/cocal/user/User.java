package cola.springboot.cocal.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Provider provider = Provider.LOCAL;

    @Column(name = "providerId", length = 255)
    private String providerId;

    @Column(name = "profileImageUrl", length = 2048)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus userStatus = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DefaultView defaultView = DefaultView.MONTH;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.ROLE_USER;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum UserStatus {
        ACTIVE,  // 활성 사용자
        BLOCKED,  // 차단한 사용자
        DELETED    // 탈퇴한 사용자
    }

    public enum DefaultView {
        MONTH,
        WEEK,
        DAY
    }

    public enum Provider {
        LOCAL,
        GOOGLE
    }

    public enum Role {
        ROLE_USER,
        ROLE_ADMIN
    }

    @Builder
    public User(String email, String name, String password, String providerId, String profileImageUrl) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.provider = provider;
        this.providerId = providerId;
        this.profileImageUrl = profileImageUrl;
        this.userStatus = UserStatus.ACTIVE;
        this.defaultView = DefaultView.MONTH;
        this.role = Role.ROLE_USER;
    }
}
