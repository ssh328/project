package com.song.project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.song.project.entity.User;
import com.song.project.exception.BadRequestException;
import com.song.project.repository.UserRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입 처리
    public RegisterResult register(String userId, String password, String username, String email) {

        if (userRepository.findByUser_id(userId).isPresent()) {
            throw new BadRequestException("이미 존재하는 아이디입니다.");
        }

        if (userId.length() < 4 || userId.length() > 20) {
            throw new BadRequestException("아이디는 4자 이상 20자 이하여야 합니다.");
        }
        // 영문, 숫자만 허용
        if (!userId.matches("^[a-zA-Z0-9]+$")) {
            throw new BadRequestException("아이디는 영문과 숫자만 사용할 수 있습니다.");
        }

        if (password.length() < 8 || password.length() > 20) {
            throw new BadRequestException("비밀번호는 8자 이상 20자 이하여야 합니다.");
        }
        // 최소 1개 이상의 영문, 숫자 포함
        if (!password.matches("^(?=.*[a-zA-Z])(?=.*[0-9]).+$")) {
            throw new BadRequestException("비밀번호는 영문과 숫자를 포함해야 합니다.");
        }

        User user = new User();
        user.setUser_id(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("default");
        user.setDp("https://javaspringproject.s3.ap-northeast-2.amazonaws.com/project/default-profile-img.png");

        User savedUser = userRepository.save(user);
        log.info("회원가입 성공: userId={}, username={}, email={}", 
            savedUser.getUser_id(), savedUser.getUsername(), savedUser.getEmail());
        return new RegisterResult(savedUser);
    }

    // DTO 클래스
    @Getter
    public static class RegisterResult {
        private final Long id;
        private final String username;
        private final String email;

        public RegisterResult(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
        }
    }
}
