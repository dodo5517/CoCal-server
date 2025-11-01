package cola.springboot.cocal.common.exception;

import cola.springboot.cocal.common.api.ApiError;
import cola.springboot.cocal.common.api.ApiResponse;
import groovy.util.logging.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    // 요청값 타입/포맷 오류
    @ExceptionHandler({ MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class, DateTimeParseException.class })
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(Exception e, HttpServletRequest req) {
        var body = ApiResponse.fail(
                ApiError.builder()
                        .code("INVALID_REQUEST_FORMAT")
                        .message("요청 값의 형식이 올바르지 않습니다.")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .details(Map.of("reason", e.getMessage()))
                        .build(),
                req.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    // 마지막 보루
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleUncaught(Exception ex, HttpServletRequest req) {
        String errorId = java.util.UUID.randomUUID().toString();
        StackTraceElement top = ex.getStackTrace().length > 0 ? ex.getStackTrace()[0] : null;
        log.error("[{}] Unhandled: {} at {}:{}",
                errorId, ex.toString(),
                top != null ? top.getClassName() : "n/a",
                top != null ? top.getLineNumber() : -1,
                ex);

        var body = ApiResponse.fail(
                ApiError.builder()
                        .code("INTERNAL_ERROR")
                        .message("서버 내부 오류가 발생했습니다. (" + errorId + ")")
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .build(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}