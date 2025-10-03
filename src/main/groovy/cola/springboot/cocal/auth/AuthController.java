package cola.springboot.cocal.auth;

import cola.springboot.cocal.common.security.JwtTokenProvider;
import cola.springboot.cocal.common.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

import static cola.springboot.cocal.common.util.CookieUtils.addRefreshCookie;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse res) {
        // Header에서 User-Agent 가져옴
        String userAgent = httpRequest.getHeader("User-Agent");

        var pair = authService.login(req.email(), req.password(), userAgent);
        // 쿠키에 RefreshToken 저장
        addRefreshCookie(res, pair.refreshToken());
        long accessExpiresIn = jwtProvider.getAccessTokenTtlSeconds(); // 예: 20분 → 1200초
        return ResponseEntity.ok(new TokenResponse(pair.accessToken(),accessExpiresIn));
    }

    // 해당 기기에서 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse res,
                                                      Authentication authentication) {
        String authHeader = request.getHeader("Authorization");
        Long userId = Long.parseLong(authentication.getName());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("message", "Authorization 헤더가 올바르지 않습니다."));
        }
        String token = authHeader.substring(7); // "Bearer " 제거
        // 해당 AccessToken을 Blacklist에 추가
        tokenBlacklistService.addToBlacklist(token);

        // Header에서 User-Agent 가져옴
        String userAgent = request.getHeader("User-Agent");
        // refreshToken revoke
        authService.logoutDevice(userId, userAgent);

        // RefreshToken 쿠키 삭제
        CookieUtils.deleteRefreshCookie(res);
        return ResponseEntity.ok(Collections.singletonMap("message", "로그아웃 되었습니다."));
    }

    // 해당 유저의 모든 기기에서 로그아웃
    @PostMapping("/all-logout")
    public ResponseEntity<Map<String, String>> allLogout(HttpServletRequest request, HttpServletResponse res,
                            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(Collections.singletonMap("messgae", refreshTokenService.logoutAll(userId)));
    }

    // accessToken 재발급
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {

        if (refreshToken == null) {
            return ResponseEntity.badRequest()
                    .body(new TokenResponse("RefreshToken이 없습니다.", 0));
        }

        try {
            // AccessToken 재발급
            AuthService.TokenPair tokens = authService.reissueAccessToken(refreshToken);

            // TTL 계산
            long accessTtl = jwtProvider.getAccessTokenTtlSeconds();
            return ResponseEntity.ok(new TokenResponse(tokens.accessToken(), accessTtl));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenResponse(e.getMessage(), 0));
        }
    }
}
