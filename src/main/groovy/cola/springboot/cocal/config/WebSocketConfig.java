package cola.springboot.cocal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


@Configuration
@EnableWebSocketMessageBroker // STOMP 메시지 브로커를 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 메시지 브로커 설정
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        // 클라이언트가 구독(subscribe)할 수 있는 목적지(prefix)
        // 예: /topic/notifications/1 처럼 userId 별 알림을 받을 수 있음

        config.setApplicationDestinationPrefixes("/app");
        // 클라이언트가 메시지를 보낼 때 prefix
        // 예: /app/notify로 메시지를 보내면 서버의 @MessageMapping("/notify")가 처리
    }

    // STOMP 엔드포인트 등록
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // 클라이언트가 연결할 WebSocket 엔드포인트
                .setAllowedOriginPatterns("*") // CORS 허용 (모든 도메인)
                .withSockJS(); // SockJS fallback 사용 (WebSocket 지원 안 되는 브라우저용)
    }
}