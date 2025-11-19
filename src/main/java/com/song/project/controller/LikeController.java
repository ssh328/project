package com.song.project.controller;

import com.song.project.CustomUser;
import com.song.project.entity.Likes;
import com.song.project.entity.Post;
import com.song.project.entity.User;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostRepository;
import com.song.project.service.RecommendedPostService;
import com.song.project.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;
}