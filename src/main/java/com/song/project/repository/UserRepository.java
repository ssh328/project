package com.song.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.song.project.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
