package cola.springboot.cocal.config;

import cola.springboot.cocal.common.security.JwtAuthFilter;
import cola.springboot.cocal.common.security.JwtTokenProvider;
import cola.springboot.cocal.common.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import cola.springboot.cocal.user.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwt;
    private final CustomOAuth2UserService customOAuth2UserService; // <- 필드 추가
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var jwtFilter = new JwtAuthFilter(jwt);

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 허용할 url
                .authorizeHttpRequests(auth -> auth
                        // 인증/토큰 관련(ex.로그인, 토큰 재발급, 로그아웃)
                        .requestMatchers("/api/auth/**").permitAll()
                        // 헬스체크/버전
                        .requestMatchers(HttpMethod.GET, "/health", "/version").permitAll()
                        // 회원가입(예: POST /api/users)
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        // OAuth2 로그인 진입/콜백 URL (소셜 로그인 필수 공개)
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // 그 외는 인증 필요.
                        .anyRequest().authenticated()
                )
                // 인증 실패 응답
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restEntryPoint()))
                // OAuth2 로그인 (소셜)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(ep -> ep.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler) // JWT 발급/쿠키/리다이렉트
                )
                // JWT 필터 등록: UsernamePasswordAuthenticationFilter 전에 실행
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint restEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Unauthorized\"}");
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration conf) throws Exception {
        return conf.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}