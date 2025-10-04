package cola.springboot.cocal.common.exception;

import cola.springboot.cocal.common.api.ApiError;
import cola.springboot.cocal.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusiness(BusinessException e, HttpServletRequest req) {
        var body = ApiResponse.fail(
                ApiError.builder()
                        .code(e.getCode())
                        .message(e.getMessage())
                        .status(e.getStatus().value())
                        .details(e.getDetails())
                        .build(),
                req.getRequestURI()
        );
        return ResponseEntity.status(e.getStatus()).body(body);
    }

    // DTO @Valid 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest req) {
        var fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of("field", err.getField(), "reason", err.getDefaultMessage()))
                .toList();

        var body = ApiResponse.fail(
                ApiError.builder()
                        .code("VALIDATION_ERROR")
                        .message("요청 값이 유효하지 않습니다.")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .details(fieldErrors)
                        .build(),
                req.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    // @Validated 파라미터/PathVariable 유효성 실패
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest req) {
        var body = ApiResponse.fail(
                ApiError.builder()
                        .code("VALIDATION_ERROR")
                        .message("요청 값이 유효하지 않습니다.")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .details(e.getConstraintViolations().stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).toList())
                        .build(),
                req.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    // 쿼리 파라미터 누락 등
    @ExceptionHandler({ MissingServletRequestParameterException.class, BindException.class, IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<ApiResponse<?>> handleBadRequest(Exception e, HttpServletRequest req) {
        var body = ApiResponse.fail(
                ApiError.builder()
                        .code("BAD_REQUEST")
                        .message(e.getMessage())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .build(),
                req.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    // 마지막 보루
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneric(Exception e, HttpServletRequest req) {
        var body = ApiResponse.fail(
                ApiError.builder()
                        .code("INTERNAL_ERROR")
                        .message("서버 내부 오류가 발생했습니다.")
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .build(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}