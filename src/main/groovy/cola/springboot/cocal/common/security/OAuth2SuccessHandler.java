package cola.springboot.cocal.common.security;

import cola.springboot.cocal.auth.RefreshTokenService;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.Pair;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest req, HttpServletResponse res, Authentication authentication) throws IOException {

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");

        // 이메일 존재 유무 확인 및 유저 정보 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일"));
        
        // AccessToken 발급
        Collection<String> roleStrings = List.of(user.getRole().name());
        String access = jwtProvider.createAccessToken(user.getId(), user.getEmail(), roleStrings);
        // expiresIn
        long accessExpiresIn = jwtProvider.getAccessTokenTtlSeconds(); // 예: 20분 → 1200초

        // RefreshToken 발급
        Pair<String, byte[]> e = jwtProvider.createRefreshToken();
        String refreshForClient = e.getFirst();
        byte[] refreshHash = e.getSecond();

        // RefreshToken db에 저장
        refreshTokenService.saveRefreshToken(user,refreshHash);


        Cookie cookie = new Cookie("refreshToken", refreshForClient);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");            // 해당 경로로만 전송
        cookie.setMaxAge(60 * 60 * 24 * 30);   // 30일
        cookie.setAttribute("SameSite", "None");  // 크로스 사이트용으로 None 사용
        res.addCookie(cookie);

        // AccessToken은 JSON Body로 응답
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("application/json;charset=UTF-8");

        String json = """
              {
                "accessToken": "%s",
                "expiresIn": %d
              }
              """.formatted(access, accessExpiresIn);

        res.getWriter().write(json);
        res.getWriter().flush();

    }
}
