package cola.springboot.cocal.common.security;

import cola.springboot.cocal.auth.RefreshTokenService;
import cola.springboot.cocal.common.api.ApiResponse;
import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.common.util.DeviceInfoParser;
import cola.springboot.cocal.user.User;
import cola.springboot.cocal.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import static cola.springboot.cocal.common.util.CookieUtils.addRefreshCookie;

@Configuration
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl; // ← 여기서 값 주입받음

    private final JwtTokenProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    public record AuthData(String accessToken, long expiresIn) {}

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest req, HttpServletResponse res, Authentication authentication) throws IOException {

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");

        // 이메일 존재 유무 확인 및 유저 정보 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "EMAIL_NOT_FOUND",
                        "존재하지 않는 이메일입니다."
                ));

        // Header에서 User-Agent 가져옴
        String userAgent = req.getHeader("User-Agent");

        // 디바이스 정보 파싱
        String deviceInfo = DeviceInfoParser.extractDeviceInfo(userAgent);
        log.debug("로그인 시도한 deviceInfo: {}", deviceInfo);

        // AccessToken 발급
        Collection<String> roleStrings = List.of(user.getRole().name());
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), roleStrings);
        // expiresIn
        long accessExpiresIn = jwtProvider.getAccessTokenTtlSeconds(); // 예: 20분 → 1200초

        // RefreshToken 발급
        Pair<String, byte[]> e = jwtProvider.createRefreshToken();
        String refreshForClient = e.getFirst();
        byte[] refreshHash = e.getSecond();
        log.debug("refreshForClient: {}", refreshForClient);
        log.debug("refreshHash: {}", refreshHash);

        // RefreshToken db에 저장
        refreshTokenService.saveRefreshToken(user, deviceInfo, refreshHash);
        addRefreshCookie(res,refreshForClient);

        // AccessToken은 JSON Body로 응답
        /*res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("application/json;charset=UTF-8");

        AuthData data = new AuthData(accessToken, accessExpiresIn);

        var successResponse = ApiResponse.ok(data, req.getRequestURI());

        objectMapper.writeValue(res.getWriter(), successResponse);*/

        // 프론트엔드로 redirect (token, expiresIn 전달)
        String redirectUrl = frontendBaseUrl + "oauth/success"
                + "?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                + "&expiresIn=" + accessExpiresIn;

        log.info("Redirecting to front-end: {}", redirectUrl);
        res.sendRedirect(redirectUrl);
    }
}
