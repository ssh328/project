package com.song.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import com.song.project.security.JwtFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize("isAuthenticated()")를 사용하기 위해 사용
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-XSRF-TOKEN");
        return repository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository())
                .ignoringRequestMatchers("/login/jwt")
                .ignoringRequestMatchers("/verify-password")
                .ignoringRequestMatchers("/change-password")
        );

        http.addFilterBefore(new JwtFilter(), ExceptionTranslationFilter.class);

        http.sessionManagement((session) -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.authorizeHttpRequests((authorize) ->
            authorize
            // 공개 경로 (인증 불필요)
            .requestMatchers(
                "/",
                "/actuator/health",
                // 인증 관련
                "/login", "/login/jwt",
                "/register", "/user-register",
                "/logout/jwt",
                
                // 게시글 조회
                "/post/list", "/post/search",
                "/post/detail/**",
                
                // 사용자 프로필 조회
                "/user/profile/**",
                
                // 정적 리소스
                "/main.css", "/pagination.css", "/star.css",
                "/js/**",
                "/favicon.ico",
                "/.well-known/**",
                
                // Swagger (dev 환경)
                "/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**",
                
                // 에러 페이지
                "/error"
            ).permitAll()
            
            // 관리자 전용
            .requestMatchers("/admin/**")
                .hasRole("ADMIN")
            
            // 나머지 모든 요청은 인증 필요
            .anyRequest().authenticated()
        );

        // 로그인 페이지 지정
        http.formLogin(form -> form
                .loginPage("/login")  // 로그인 페이지 URL
                .permitAll()
        );

        http.exceptionHandling(exception -> exception
        .authenticationEntryPoint((request, response, authException) -> {
            // 로그인 페이지로 리다이렉트
            response.sendRedirect("/login?error=login_required");
            })
        );

        return http.build();
    }
}
