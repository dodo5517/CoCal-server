package cola.springboot.cocal.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtils {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final int REFRESH_COOKIE_MAX_AGE = 60 * 60 * 24 * 30; // 30일

    public static void addRefreshCookie(HttpServletResponse res, String token) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth"); // 해당 경로로만 전송
        cookie.setMaxAge(REFRESH_COOKIE_MAX_AGE);
        cookie.setAttribute("SameSite", "None"); // 크로스 사이트용
        res.addCookie(cookie);
    }

    public static void deleteRefreshCookie(HttpServletResponse res) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0); // 즉시 만료
        cookie.setAttribute("SameSite", "None");
        res.addCookie(cookie);
    }
}
