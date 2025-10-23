package cola.springboot.cocal.common.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            String token = httpServletRequest.getParameter("token");

            if (token == null || token.isBlank()) {
                log.warn("WebSocket 연결 실패: token 누락");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            if (!jwtTokenProvider.isValid(token)) {
                log.warn("WebSocket 연결 실패: token 검증 실패");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            Long userId = jwtTokenProvider.getUserId(token);
            attributes.put("userId", userId); // 세션 attribute에 저장
            log.info("WebSocket 연결 승인 - userId: {}", userId);
            return true;
        }

        return false;
    }
    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // handshake 이후 처리 (보통 비워둠)
    }
}