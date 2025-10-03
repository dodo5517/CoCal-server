package cola.springboot.cocal.auth;

public record TokenResponse(String accessToken, long expiresIn) {}
