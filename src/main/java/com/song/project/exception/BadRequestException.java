package com.song.project.exception;

// 잘못된 요청(400 Bad Request)에 대한 커스텀 예외 클래스
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
