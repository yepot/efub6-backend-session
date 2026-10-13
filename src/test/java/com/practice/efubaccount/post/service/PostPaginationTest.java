package com.practice.efubaccount.post.service;

import com.practice.efubaccount.account.domain.Account;
import com.practice.efubaccount.account.service.AccountService;
import com.practice.efubaccount.post.domain.Post;
import com.practice.efubaccount.post.dto.response.PostPageResponse;
import com.practice.efubaccount.post.dto.summary.PostSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(PostService.class)
class PostPaginationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PostService postService;

    @MockitoBean
    private AccountService accountService;

    private final List<Long> postIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Account firstWriter = entityManager.persist(Account.builder()
                .email("first@example.com").password("password").nickname("first").build());
        Account secondWriter = entityManager.persist(Account.builder()
                .email("second@example.com").password("password").nickname("second").build());
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);

        for (int index = 0; index < 5; index++) {
            Post post = entityManager.persistAndFlush(Post.builder()
                    .writer(index % 2 == 0 ? firstWriter : secondWriter)
                    .title("post-" + index).content("content").build());
            postIds.add(post.getId());
            // 같은 생성 시각에서 id 정렬로 페이지 경계가 안정적인지 확인한다.
            entityManager.getEntityManager().createQuery(
                            "UPDATE Post p SET p.createdAt = :createdAt, p.viewCount = :viewCount WHERE p.id = :id")
                    .setParameter("createdAt", index == 1 ? createdAt.plusDays(1) : createdAt)
                    .setParameter("viewCount", (long) index)
                    .setParameter("id", post.getId())
                    .executeUpdate();
        }
        entityManager.clear();
    }

    @Test
    void getPosts_preservesOrderingProjectionAndPageMetadata() {
        PostPageResponse first = postService.getPosts(0, 2);
        PostPageResponse second = postService.getPosts(1, 2);
        PostPageResponse last = postService.getPosts(2, 2);

        assertThat(first.posts()).containsExactly(
                new PostSummary(postIds.get(1), "second", "post-1", 1L),
                new PostSummary(postIds.get(4), "first", "post-4", 4L));
        assertThat(second.posts()).extracting(PostSummary::postId)
                .containsExactly(postIds.get(3), postIds.get(2));
        assertThat(last.posts()).extracting(PostSummary::postId).containsExactly(postIds.get(0));
        assertThat(first.currentPage()).isZero();
        assertThat(second.currentPage()).isEqualTo(1);
        assertThat(last.currentPage()).isEqualTo(2);
        assertThat(first.pageSize()).isEqualTo(2);
        assertThat(first.totalPosts()).isEqualTo(5);
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(first.hasNext()).isTrue();
        assertThat(second.hasNext()).isTrue();
        assertThat(last.totalPosts()).isEqualTo(5);
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    void getPosts_returnsTotalCountForPageBeyondLast() {
        PostPageResponse page = postService.getPosts(3, 2);

        assertThat(page.posts()).isEmpty();
        assertThat(page.currentPage()).isEqualTo(3);
        assertThat(page.totalPosts()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasNext()).isFalse();
    }
}
