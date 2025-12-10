package com.song.project.exception;

// 찾을 수 없음(404 Not Found)에 대한 커스텀 예외 클래스
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
