package cola.springboot.cocal.common.api;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiError {
    private final String code;    // 팀 표준 에러코드 (예: INVITE_TOO_MANY_DECLINES)
    private final String message; // 사용자 노출 메시지
    private final Integer status; // HTTP Status (400/403/409/500 등)
    private final Object details; // 선택: 필드 에러 목록, 추가 정보 등

    public ApiError(String code, String message, Integer status, Object details) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.details = details;
    }
}
