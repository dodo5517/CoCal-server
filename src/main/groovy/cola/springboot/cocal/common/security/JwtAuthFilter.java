package cola.springboot.cocal.common.security;

import cola.springboot.cocal.auth.TokenBlacklistService;
import cola.springboot.cocal.common.api.ApiResponse;
import cola.springboot.cocal.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.List;

import static cola.springboot.cocal.common.exception.ErrorMappers.toApiError;

@Component
@RequiredArgsConstructor
// 요청 1건당 한 번 실행됨.
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String auth = req.getHeader("Authorization");

        jwt.resolveFromAuthorization(auth).ifPresent(token -> {
            try {
                //  로그아웃된(블랙리스트) 토큰인지 체크
                if (tokenBlacklistService.isBlacklisted(token)) {
                    SecurityContextHolder.clearContext();
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    BusinessException ex = new BusinessException(
                            HttpStatus.UNAUTHORIZED,
                            "TOKEN_BLACKLISTED",
                            "로그아웃된 토큰입니다."
                    );
                    String path = req.getRequestURI();
                    // BusinessException → ApiError 변환
                    var failResponse = ApiResponse.fail(toApiError(ex), path);
                    new ObjectMapper().writeValue(res.getWriter(), failResponse);
                    return; // 여기서 필터 중단
                }

                // 정상 토큰 처리
                Claims c = jwt.getClaims(token); // (검증 포함)
                Long userId = jwt.getUserId(token);

                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) c.get("roles");
                var authorities = roles == null
                        ? List.<SimpleGrantedAuthority>of()
                        : roles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .toList();

                var authToken = new AbstractAuthenticationToken(authorities) {
                    @Override public Object getCredentials() { return token; }
                    @Override public Object getPrincipal() { return userId; }
                };
                authToken.setAuthenticated(true);

                SecurityContextHolder.getContext().setAuthentication(authToken);
                req.setAttribute("userId", userId);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        });

        // 다음 필터나 컨트롤러로 진행
        if (!res.isCommitted()) {
            chain.doFilter(req, res);
        }
    }
}
