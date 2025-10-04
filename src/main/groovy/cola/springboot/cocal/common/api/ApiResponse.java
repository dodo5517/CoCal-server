package cola.springboot.cocal.common.api;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final ApiError error;
    private final String serverTime; // ISO-8601
    private final String path;       // 요청 URI

    public static <T> ApiResponse<T> ok(T data, String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .error(null)
                .serverTime(java.time.OffsetDateTime.now().toString())
                .path(path)
                .build();
    }

    public static <T> ApiResponse<T> fail(ApiError error, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .data(null)
                .error(error)
                .serverTime(java.time.OffsetDateTime.now().toString())
                .path(path)
                .build();
    }
}