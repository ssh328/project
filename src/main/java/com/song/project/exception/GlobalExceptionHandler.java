package com.song.project.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private boolean isApiRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        return (accept != null && accept.contains("application/json")) ||
               (requestedWith != null && requestedWith.equalsIgnoreCase("XMLHttpRequest"));
    }

    private ResponseEntity<Map<String, Object>> json(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Object handleUnauthorized(UnauthorizedException e, RedirectAttributes ra, HttpServletRequest req) {
        if (isApiRequest(req)) return json(HttpStatus.UNAUTHORIZED, e.getMessage());
        ra.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/list";
    }

    @ExceptionHandler(ForbiddenException.class)
    public Object handleForbidden(ForbiddenException e, RedirectAttributes ra, HttpServletRequest req) {
        if (isApiRequest(req)) return json(HttpStatus.FORBIDDEN, e.getMessage());
        ra.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/list";
    }

    @ExceptionHandler(NotFoundException.class)
    public Object handleNotFound(NotFoundException e, RedirectAttributes ra, HttpServletRequest req) {
        if (isApiRequest(req)) return json(HttpStatus.NOT_FOUND, e.getMessage());
        ra.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/list";
    }

    @ExceptionHandler(BadRequestException.class)
    public Object handleBadRequest(BadRequestException e, RedirectAttributes ra, HttpServletRequest req) {
        if (isApiRequest(req)) return json(HttpStatus.BAD_REQUEST, e.getMessage());
        ra.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/list";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", "유효성 검사 실패");
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return json(HttpStatus.BAD_REQUEST, "요청 파라미터 타입이 올바르지 않습니다.");
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception e, RedirectAttributes ra, HttpServletRequest req) {
        String errorMsg = e.getMessage();
        if (isApiRequest(req)) return json(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
        ra.addFlashAttribute("errorMessage", errorMsg);
        return "redirect:/list";
    }
}