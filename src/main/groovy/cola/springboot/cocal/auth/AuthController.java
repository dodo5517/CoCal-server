package cola.springboot.cocal.auth;

import cola.springboot.cocal.common.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("message", "Authorization 헤더가 올바르지 않습니다."));
        }

        String token = authHeader.substring(7); // "Bearer " 제거
        tokenBlacklistService.addToBlacklist(token);

        return ResponseEntity.ok(Collections.singletonMap("message", "로그아웃 되었습니다."));
    }
}
