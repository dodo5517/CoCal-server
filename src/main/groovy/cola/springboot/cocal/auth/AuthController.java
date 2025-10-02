package cola.springboot.cocal.auth;

import jakarta.servlet.http.Cookie;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req,
                                               HttpServletResponse res) {
        var pair = authService.login(req.email(), req.password());
        attachRefreshCookie(res, pair.refreshToken());
        return ResponseEntity.ok(new TokenResponse(pair.accessToken()));
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

    private static void attachRefreshCookie(HttpServletResponse res, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");            // 재발급/로그아웃 경로만 전송
        cookie.setMaxAge(60 * 60 * 24 * 30);   // 30일
        cookie.setAttribute("SameSite", "None"); // 크로스 사이트용으로 None 사용
        res.addCookie(cookie);
    }
}
