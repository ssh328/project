package com.song.project.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.song.project.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 사용자명으로 사용자 조회
    Optional<User> findByUsername(String username);

    // Full text 검색(username 또는 email)
    @Query(value = "select * from store.user where match(username, email) against(?1)",
            countQuery = "select count(*) from store.user where match(username, email) against(?1)",
            nativeQuery = true)
    Page<User> fullTextSearchUsernameOrEmail(String keyword, Pageable pageable);

    // 사용자 ID로 사용자 조회
    @Query("SELECT u FROM User u WHERE u.user_id = :user_id")
    Optional<User> findByUser_id(@Param("user_id") String user_id);
}
