package com.song.project.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.song.project.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    // Full text 검색(username 또는 email)
    @Query(value = "select * from store.user where match(username, email) against(?1)",
            countQuery = "select count(*) from store.user where match(username, email) against(?1)",
            nativeQuery = true)
    Page<User> fullTextSearchUsernameOrEmail(String keyword, Pageable pageable);
}
