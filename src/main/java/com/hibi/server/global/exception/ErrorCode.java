package com.hibi.server.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // --- 인증/인가 (Authentication/Authorization) 관련 에러 ---
    // JWT 토큰 관련
    JWT_INVALID_TOKEN("A001", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    JWT_EXPIRED_TOKEN("A002", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    JWT_UNSUPPORTED_TOKEN("A003", "지원하지 않는 토큰 형식입니다.", HttpStatus.UNAUTHORIZED),
    JWT_SIGNATURE_INVALID("A004", "토큰 서명이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
    JWT_MISSING_TOKEN("A005", "토큰이 누락되었습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("A006", "리프레시 토큰을 찾을 수 없습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("A007", "유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
    REPLAY_ATTACK("A008", "만료된 토큰을 사용하였습니다.", HttpStatus.UNAUTHORIZED),

    // 인증 과정 관련
    AUTHENTICATION_FAILED("A010", "인증에 실패했습니다.", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED_ACCESS("A011", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN), // 권한 부족
    BAD_CREDENTIALS("A012", "아이디 또는 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),

    // 회원 가입 관련
    EMAIL_ALREADY_EXISTS("A020", "이미 등록된 이메일입니다.", HttpStatus.CONFLICT), // 409 Conflict
    NICKNAME_ALREADY_EXISTS("A021", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    INVALID_PASSWORD_PATTERN("A022", "비밀번호 형식이 올바르지 않습니다.", HttpStatus.BAD_REQUEST), // 400 Bad Request
    EMAIL_REQUIRED("A023", "이메일은 필수 입력 값입니다.", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_FORMAT("A024", "유효하지 않은 이메일 형식입니다.", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED("A025", "비밀번호는 필수 입력 값입니다.", HttpStatus.BAD_REQUEST),
    PASSWORD_TOO_SHORT("A026", "비밀번호는 최소 8자 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    NICKNAME_REQUIRED("A027", "닉네임은 필수 입력 값입니다.", HttpStatus.BAD_REQUEST),
    NICKNAME_INVALID_LENGTH("A028", "닉네임은 2자 이상 20자 이하이어야 합니다.", HttpStatus.BAD_REQUEST),

    //post 관련 에러 코드
    POST_ALREADY_EXISTS("P001", "해당 날짜에 이미 포스트가 존재합니다.", HttpStatus.CONFLICT),

    // --- 회원 관련 에러 ---
    MEMBER_NOT_FOUND("M001", "회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // --- 신고 관련 에러 (F11) ---
    REPORT_NOT_FOUND("R001", "신고를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SELF_REPORT_NOT_ALLOWED("R002", "본인 콘텐츠는 신고할 수 없습니다.", HttpStatus.BAD_REQUEST),
    DUPLICATE_REPORT("R003", "이미 신고한 콘텐츠입니다.", HttpStatus.CONFLICT),
    REPORT_TARGET_NOT_FOUND("R004", "신고 대상을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // --- 문의 관련 에러 (F10) ---
    QUESTION_NOT_FOUND("Q001", "문의를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // --- FAQ 관련 에러 (F9) ---
    FAQ_NOT_FOUND("F001", "FAQ를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // --- 일반적인 에러 코드 ---
    INVALID_INPUT_VALUE("C001", "잘못된 입력 값입니다.", HttpStatus.BAD_REQUEST), // 400 Bad Request
    ENTITY_NOT_FOUND("C002", "요청하신 자원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND), // 404 Not Found
    ALREADY_EXISTS("C003", "이미 존재합니다.", HttpStatus.CONFLICT), // 409 Conflict
    DUPLICATE_ENTITY("C004", "중복된 항목입니다.", HttpStatus.CONFLICT), // 409 Conflict
    INTERNAL_SERVER_ERROR("S001", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR); // 500 Internal Server Error


    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}