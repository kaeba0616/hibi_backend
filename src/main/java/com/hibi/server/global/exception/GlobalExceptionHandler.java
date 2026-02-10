package com.hibi.server.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${api.base-url}")
    private String apiBaseUrl;

    private ProblemDetail createProblemDetail(HttpStatus status, ErrorCode errorCode) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, errorCode.getMessage());
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setType(URI.create("http://" + apiBaseUrl + getProblemTypePath(errorCode)));

        try {
            problemDetail.setInstance(URI.create(ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUriString()));
        } catch (IllegalStateException e) {
            log.warn("요청 경로 요청 중 오류가 발생했습니다. {}", e.getMessage());
            problemDetail.setInstance(URI.create("http://" + apiBaseUrl + "/errors/unknown-instance"));
        }

        problemDetail.setProperty("errorCode", errorCode.getCode());
        return problemDetail;
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ProblemDetail> handleCustomException(CustomException ex) {
        log.error(ex.getMessage());
        ProblemDetail problemDetail = createProblemDetail(ex.getStatus(), ex.getErrorCode());
        return ResponseEntity.status(ex.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        log.error("[MethodArgumentNotValidException] : {}", ex.getMessage());
        ProblemDetail problemDetail = createProblemDetail(status, ErrorCode.INVALID_INPUT_VALUE);
        return ResponseEntity.status(status).body(problemDetail);
    }

    // Spring Security 인증 관련 예외 처리
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ProblemDetail> handleAuthenticationException(AuthenticationException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        log.error("[AuthenticationException] : {}", ex.getMessage());
        ProblemDetail problemDetail = createProblemDetail(status, ErrorCode.AUTHENTICATION_FAILED);
        return ResponseEntity.status(status).body(problemDetail);
    }

    // Spring Security 권한 관련 예외 처리 (403 Forbidden)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        log.error("[AccessDeniedException] : {}", ex.getMessage());
        ProblemDetail problemDetail = createProblemDetail(status, ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.status(status).body(problemDetail);
    }

    // --- 클라이언트 요청 유효성 검사 관련 예외 처리 ---
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        log.error("[HttpMessageNotReadableException] : {}", e.getMessage());
        ProblemDetail problemDetail = createProblemDetail(status, ErrorCode.INVALID_INPUT_VALUE);
        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAllExceptions(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error("[Internal Server Error] : {}", ex.getMessage(), ex);
        ProblemDetail problemDetail = createProblemDetail(status, ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(status).body(problemDetail);
    }

    private String getProblemTypePath(ErrorCode errorCode) {
//        if (errorCode != null && errorCode.getCode() != null && !errorCode.getCode().isEmpty()) {
//            char category = errorCode.getCode().charAt(0);
//            path = switch (category) {
//                case 'A' -> "/problems/auth-error"; // 인증/인가 오류
//                case 'C' -> "/problems/common-error"; // 일반 클라이언트 오류
//                case 'S' -> "/problems/serv   er-error"; // 서버 오류
//                default -> "/problems/other-error";
//            };
//        }
        //TODO: swagger 문서 작성 완료 시 index가 아닌 특정 항목으로 이동할 수 있게 변경
//        return ":8080/swagger-ui/index.html#/problems/" + errorCode.getCode();
        return ":8080/swagger-ui/index.html#/";
    }
}