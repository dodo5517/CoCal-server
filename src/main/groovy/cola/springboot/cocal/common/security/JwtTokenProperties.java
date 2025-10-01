package cola.springboot.cocal.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
// @ConfigurationProperties은 자동으로 bean 등록 안 해줌.
// -> SecurityPropsConfig 클래스가 직접 bean으로 등록시키는 게 권장이지만 @Component로 주입시켰음.
@ConfigurationProperties(prefix = "app.jwt")
public class JwtTokenProperties {
    private String issuer;
    private String audience;
    private int accessTtlMinutes = 20;
    private int clockSkewSeconds = 30;
    private String secretBase64;
}
