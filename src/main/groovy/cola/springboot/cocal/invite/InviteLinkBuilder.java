package cola.springboot.cocal.invite;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InviteLinkBuilder {
    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public String build(String token) {
        return String.format("%s/invite/%s", trimSlash(frontendBaseUrl), token);
    }

    private String trimSlash(String s) {
        if (s.endsWith("/")) return s.substring(0, s.length()-1);
        return s;
    }
}