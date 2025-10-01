package cola.springboot.cocal.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@RequiredArgsConstructor
// 요청 1건당 한 번 실행됨.
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwt;


    @Override
    // 토큰을 읽고 SecurityContext를 채우는 필터.
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws java.io.IOException, jakarta.servlet.ServletException {
        // Authorization 헤더 읽기
        String auth = req.getHeader("Authorization");
        // "Bearer " 뒤의 순수 토큰만 Optional로 변환
        jwt.resolveFromAuthorization(auth).ifPresent(token -> {
            try {
                Claims c = jwt.getClaims(token); // 검증 포함
                Long userId = jwt.getUserId(token);
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) c.get("roles");
                var authorities = roles == null ? List.<SimpleGrantedAuthority>of()
                        : roles.stream().map(r-> new SimpleGrantedAuthority("ROLE_"+r)).toList();

                // 익명 클래스로 인증 토큰 생성
                var authToken = new AbstractAuthenticationToken(authorities) {
                    @Override public Object getCredentials() { return token; }
                    @Override public Object getPrincipal() { return userId; }
                };
                // 이미 검증 완료된 인증임을 true로 표시
                authToken.setAuthenticated(true);
                // SecurityContext에 위의 인증 객체 넣음.
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // 요청 속성으로도 제공(혹시 필요할까 싶어서 추가)
                req.setAttribute("userId", userId);
            } catch (Exception ignored) {
                // 무효 토큰이면 컨텍스트 비움(401 처리는 EntryPoint에서)
                SecurityContextHolder.clearContext();
            }
        });

        // 다음 필터나 컨트롤러로 넘김.
        chain.doFilter(req, res);
    }
}
