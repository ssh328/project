package com.song.project.exception;

// 인증 실패(401 Unauthorized)에 대한 커스텀 예외 클래스
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}