package com.practice.efubaccount.post.repository;

import com.practice.efubaccount.post.domain.Post;
import com.practice.efubaccount.post.dto.summary.PostSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // LIMIT을 JOIN 안쪽에 적용해 페이지에 필요한 게시글만 작성자와 결합한다.
    @Query(
            value = """
                    SELECT p.id, a.nickname, p.title, p.view_count
                    FROM (
                        SELECT id, writer_account_id, title, view_count, created_at
                        FROM post
                        ORDER BY created_at DESC, id DESC
                        LIMIT :size OFFSET :offset
                    ) p
                    JOIN accounts a ON a.account_id = p.writer_account_id
                    ORDER BY p.created_at DESC, p.id DESC
                    """,
            nativeQuery = true
    )
    List<PostSummary> findPostSummaries(@Param("offset") long offset, @Param("size") int size);

    Optional<Post> findById(Long id);

    // 생성한 날짜 기준 전체 조회
    List<Post> findAllByOrderByCreatedAtDesc();

    // 조회수 증가
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Post p
        SET p.viewCount = p.viewCount + 1
        WHERE p.id = :postId
        """)
    int increaseViewCount(@Param("postId") Long postId);
}
