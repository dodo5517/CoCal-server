package cola.springboot.cocal.common.exception;

public final class ErrorMappers {
    private ErrorMappers() {}

    public static cola.springboot.cocal.common.api.ApiError toApiError(BusinessException ex) {
        return new cola.springboot.cocal.common.api.ApiError(
                ex.getCode(),                       // 예: "AUTH_UNAUTHORIZED"
                ex.getMessage(),                    // 예: "인증되지 않은 요청입니다."
                ex.getStatus().value(),             // 예: 401
                ex.getDetails()                     // null 또는 Map/객체 등
        );
    }
}