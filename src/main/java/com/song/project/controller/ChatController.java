package com.song.project.controller;
import com.song.project.CustomUser;
import com.song.project.entity.User;
import com.song.project.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@Controller
public class ChatController {
    @Autowired
    UserRepository userRepository;

    @GetMapping("/chat")
    String chat(Model model, @RequestParam(required = false) Long postWriterId, Authentication auth) {

        if (auth != null && auth.isAuthenticated()) {
            CustomUser user = (CustomUser) auth.getPrincipal();

            model.addAttribute("userId", user.id);

            if (postWriterId != null) {
                model.addAttribute("postWriterId", postWriterId);
            } else {
                model.addAttribute("postWriterId", null);
            }

            System.out.println("userid :" + user.id);
            System.out.println("postWriterId :" + postWriterId);
        }
        
        return "chat.html";
    }

    // @PostMapping(value = "/createUser")
    // @ResponseBody
    // public ResponseEntity<User> createUser(@RequestBody User user) {
    //     return new ResponseEntity<>(userRepository.save(user), HttpStatus.OK);
    // }

    @GetMapping(value = "/getUser")
    @ResponseBody
    // public ResponseEntity<User> getUser(@RequestParam(required = true) Long userId) {
    //     return new ResponseEntity<>(userRepository.findById(userId).get(), HttpStatus.OK);
    // }
    public ResponseEntity<UserDto> getUser(@RequestParam(required = false) Long userId) {
        User user = userRepository.findById(userId).get();
        UserDto userDto = new UserDto(user);
        return new ResponseEntity<>(userDto, HttpStatus.OK);
    }

    // 내부 static DTO 클래스 선언
    public static class UserDto {
        private Long id;
        private String username;
        private String user_id;
        private String email;
        private String dp;
        private String role;

        public UserDto(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.user_id = user.getUser_id();
            this.email = user.getEmail();
            this.dp = user.getDp();
            this.role = user.getRole();
        }

        // getter만 필요하면 lombok 없이 직접 작성
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getUser_id() { return user_id; }
        public String getEmail() { return email; }
        public String getDp() { return dp; }
        public String getRole() { return role; }
    }
}