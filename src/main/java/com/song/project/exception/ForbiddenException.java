package com.song.project.exception;

// 권한 없음(403 Forbidden)에 대한 커스텀 예외 클래스
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
