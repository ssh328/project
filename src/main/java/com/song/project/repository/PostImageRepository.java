package com.song.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.song.project.entity.PostImage;

public interface PostImageRepository extends JpaRepository<PostImage, Long>{
    
}
