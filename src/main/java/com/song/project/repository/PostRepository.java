package com.song.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.song.project.entity.Post;
import com.song.project.entity.PostStatus;

import org.springframework.data.jpa.repository.EntityGraph;

public interface PostRepository extends JpaRepository<Post, Long> {
    // 제목으로 검색
    Page<Post> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "images"})
    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.deleted = false")
    Optional<Post> findActiveById(@Param("id") Long id);

    // 필터링된 게시물 목록 조회
    @EntityGraph(attributePaths = {"images", "user"})
    @Query("""
        SELECT p FROM Post p
        WHERE p.deleted = false
            AND (:category IS NULL OR :category = '' OR p.category = :category)
            AND (:startPrice IS NULL OR p.price >= :startPrice)
            AND (:endPrice IS NULL OR p.price <= :endPrice)
            AND (:status IS NULL OR p.status = :status)
    """)
    Page<Post> findWithFilter(@Param("category") String category,
                              @Param("startPrice") Integer startPrice,
                              @Param("endPrice") Integer endPrice,
                              @Param("status") PostStatus status,
                              Pageable pageable);

    @EntityGraph(attributePaths = {"images", "user"})
    @Query("""
        SELECT p FROM Post p
        WHERE (:category IS NULL OR :category = '' OR p.category = :category)
            AND (:startPrice IS NULL OR p.price >= :startPrice)
            AND (:endPrice IS NULL OR p.price <= :endPrice)
            AND (:status IS NULL OR p.status = :status)
    """)
    Page<Post> findAdminWithFilter(@Param("category") String category,
                                   @Param("startPrice") Integer startPrice,
                                   @Param("endPrice") Integer endPrice,
                                   @Param("status") PostStatus status,
                                   Pageable pageable);
    
    @Query(value = """
    select * from store.post
    where deleted = 0
        and match(title) against(?1)
    """,
    countQuery = """
    select count(*) from store.post
    where deleted = 0
        and match(title) against(?1)
    """,
    nativeQuery = true)
    Page<Post> fullTextSearchActiveWithPaging(String text, Pageable pageable);
    
    Page<Post> findByUserIdAndDeletedFalseOrderByIdDesc(Long userId, Pageable pageable);
    
    @EntityGraph(attributePaths = {"user", "images"})
    
    Page<Post> findByUser_UsernameAndDeletedFalse(String username, Pageable pageable);
    @EntityGraph(attributePaths = {"user", "images"})
    @Query("SELECT p FROM Post p WHERE p.id IN :ids AND p.deleted = false")
    List<Post> findAllActiveByIdIn(@Param("ids") Iterable<Long> ids);

    // 주어진 ID 목록에 해당하는 게시물 조회
    @EntityGraph(attributePaths = {"user", "images"})
    @Override
    List<Post> findAllById(Iterable<Long> ids);

    // 주어진 ID에 해당하는 게시물 조회
    @EntityGraph(attributePaths = {"user", "images"})
    @Override
    Optional<Post> findById(Long id);
}