package com.song.project.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.song.project.entity.Post;
import com.song.project.post.PostStatus;

import org.springframework.data.jpa.repository.EntityGraph;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findPageBy(Pageable page);

    @Query(value = "select * from store.post where match(title) against(?1)",
            countQuery = "select count(*) from store.post where match(title) against(?1)",
            nativeQuery = true)
    Page<Post> fullTextSearchWithPaging(String text, Pageable pageable);

    Page<Post> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    Page<Post> findByCategory(String category, Pageable pageable);

    Page<Post> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @EntityGraph(attributePaths = {"images", "user"})
    @Query("""
        SELECT p FROM Post p
        WHERE (:category IS NULL OR :category = '' OR p.category = :category)
          AND (:startPrice IS NULL OR p.price >= :startPrice)
          AND (:endPrice IS NULL OR p.price <= :endPrice)
          AND (:status IS NULL OR p.status = :status)
    """)
    Page<Post> findWithFilter(@Param("category") String category,
                              @Param("startPrice") Integer startPrice,
                              @Param("endPrice") Integer endPrice,
                              @Param("status") PostStatus status,
                              Pageable pageable);

    @EntityGraph(attributePaths = {"user", "images"})
    Page<Post> findByUser_Username(String username, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "images"})
    @Override
    java.util.List<Post> findAllById(java.lang.Iterable<Long> ids);

    @EntityGraph(attributePaths = {"user", "images"})
    @Override
    java.util.Optional<Post> findById(Long id);
}