package cola.springboot.cocal.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.data.util.Pair;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtTokenProperties props;

    // 키 생성 메서드
    private Key signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(props.getSecretBase64());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // AccessToken 생성
    public String createAccessToken(Long userId, String email, Collection<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getAccessTtlMinutes(), ChronoUnit.MINUTES);

        // JWT의 payload 부분에 들어갈 데이터 담는 Map
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("email", email);
        if (roles != null && !roles.isEmpty()) {
            claims.put("roles", roles);
        }

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)   // typ: JWT
                .setIssuer(props.getIssuer())                   // iss: 발급자
                .setAudience(props.getAudience())               // aud: 대상자
                .setSubject(String.valueOf(userId))             // sub: userId
                .setId(UUID.randomUUID().toString())            // jti (토큰 식별자)
                .setIssuedAt(Date.from(now))                    // iat: 발급시간
                .setNotBefore(Date.from(now))                   // nbf:이 전에는 유효X
                .setExpiration(Date.from(exp))                  // exp: 만료시간
                .addClaims(claims)                              // 커스텀 클레임
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // RefreshToken 생성
    public Pair<String, byte[]> createRefreshToken() {
        byte[] refreshRaw = randomBytes(32);
        String refreshForClient = base64Url(refreshRaw);
        byte[] refreshHash = sha256(refreshRaw);
        return Pair.of(refreshForClient, refreshHash);
    }
    // RefreshToken 생성 내부 유틸
    private static byte[] randomBytes(int len) {
        byte[] b = new byte[len];
        new SecureRandom().nextBytes(b);
        return b;
    }
    private static String base64Url(byte[] raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }
    private static byte[] base64UrlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }
    private static byte[] sha256(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 미지원", e);
        }
    }

    // 토큰 파싱(검증포함). 유효하지 않으면 JwtException 예외발생
    public Jws<Claims> parseAndValidate(String token) throws JwtException {
        return Jwts.parser()
                .requireAudience(props.getAudience())
                .requireIssuer(props.getIssuer())
                .setAllowedClockSkewSeconds(props.getClockSkewSeconds())
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token);
    }

    // 유효성 검사 결과만 boolean으로 보고 싶을 때 사용.
    public boolean isValid(String token) {
        try {
            parseAndValidate(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 만료 여부만 빠르게 확인하고 싶을 때 (서명/issuer/aud도 검증됨)
    public boolean isExpired(String token) {
        try {
            Jws<Claims> jws = parseAndValidate(token);
            Date exp = jws.getBody().getExpiration();
            return exp != null && exp.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return false; // 위조/형식오류 등은 만료와 구분
        }
    }


    // Claims 꺼내기(검증 포함)
    public Claims getClaims(String token) throws JwtException {
        return parseAndValidate(token).getBody();
    }

    // subject(userId) 가져오기
    public Long getUserId(String token) throws JwtException {
        Claims c = getClaims(token);
        // 우선 uid(숫자) → 없으면 subject(문자열) 사용
        Object uid = c.get("uid");
        if (uid instanceof Number n) return n.longValue();
        return Long.parseLong(c.getSubject());
    }

    // Authorization 헤더에서 Bearer 토큰만 추출
    public Optional<String> resolveFromAuthorization(String authorizationHeader) {
        if (authorizationHeader == null) return Optional.empty();
        String prefix = "Bearer ";
        if (authorizationHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return Optional.of(authorizationHeader.substring(prefix.length()).trim());
        }
        return Optional.empty();
    }
}
