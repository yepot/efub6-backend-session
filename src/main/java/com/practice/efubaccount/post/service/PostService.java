package com.practice.efubaccount.post.service;

import com.practice.efubaccount.account.domain.Account;
import com.practice.efubaccount.account.service.AccountService;
import com.practice.efubaccount.global.exception.CustomException;
import com.practice.efubaccount.global.exception.ErrorCode;
import com.practice.efubaccount.post.domain.Post;
import com.practice.efubaccount.post.dto.request.PostCreateRequest;
import com.practice.efubaccount.post.dto.request.PostUpdateRequest;
import com.practice.efubaccount.post.dto.response.PostListResponse;
import com.practice.efubaccount.post.dto.response.PostPageResponse;
import com.practice.efubaccount.post.dto.response.PostResponse;
import com.practice.efubaccount.post.dto.summary.PostSummary;
import com.practice.efubaccount.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final AccountService accountService;

    @Transactional
    public Long createPost(PostCreateRequest request) {
        Account writerAccount = accountService.findByAccountId(request.getAccountId());

        Post newPost = request.toEntity(writerAccount);
        postRepository.save(newPost);
        return newPost.getId();
    }

    @Transactional
    public PostResponse getPost(Long postId) {
        // 조회수 증가
        postRepository.increaseViewCount(postId);

        Post post = findByPostId(postId);
        return PostResponse.from(post);
    }

    @Transactional(readOnly = true)
    public PostListResponse getAllPosts() {
        List<PostSummary> postSummaries = postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PostSummary::from).toList();
        return new PostListResponse(postSummaries, postRepository.count());
    }

    @Transactional(readOnly = true)
    public PostPageResponse getPosts(int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        List<PostSummary> postSummaries = postRepository.findPostSummaries(pageRequest.getOffset(), size);
        Page<PostSummary> posts = new PageImpl<>(postSummaries, pageRequest, postRepository.count());
        return PostPageResponse.from(posts);
    }

    @Transactional
    public void updatePostContent(Long postId, PostUpdateRequest request, Long accountId) {
        Post post = findByPostId(postId);
        Account account = accountService.findByAccountId(accountId);

        authorizePostWriter(post, account);
        post.changeContent(request.content());
    }

    @Transactional
    public void deletePost(Long postId, Long accountId) {
        Post post = findByPostId(postId);
        Account account = accountService.findByAccountId(accountId);

        authorizePostWriter(post, account);
        postRepository.delete(post);
    }

    public Post findByPostId(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(()-> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private void authorizePostWriter(Post post, Account account) {
        if(!post.getWriter().equals(account)) {
            throw new CustomException(ErrorCode.POST_ACCOUNT_MISMATCH);
        }
    }
}
