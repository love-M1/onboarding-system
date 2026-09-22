package cn.edu.nuc.onboarding.system.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBusinessException(BizException exception) {
        return ApiResponse.failure(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> Objects.requireNonNullElse(error.getDefaultMessage(), "参数校验失败"))
                .orElse("参数校验失败");
        return ApiResponse.failure(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ApiResponse<Void> handleRequestException(Exception exception) {
        if (exception instanceof HttpMessageNotReadableException
                && exception.getMessage() != null
                && exception.getMessage().contains("entryTime")) {
            return ApiResponse.failure(ErrorCode.EMPLOYEE_ENTRY_TIME_INVALID, "入职日期格式不正确");
        }
        return ApiResponse.failure(ErrorCode.BAD_REQUEST, "请求参数缺失或格式不正确");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse<Void> handleNotFound() {
        return ApiResponse.failure(ErrorCode.NOT_FOUND, "访问的接口或资源不存在");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUploadSize() {
        return ApiResponse.failure(ErrorCode.TASK_FILE_TOO_LARGE, "单个附件不能超过10MB");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        log.error("Unexpected error while handling {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ApiResponse.failure(ErrorCode.SERVER_ERROR, "系统内部错误，请稍后重试");
    }
}
