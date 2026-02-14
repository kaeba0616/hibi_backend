package com.hibi.server.e2e;

import com.hibi.server.domain.auth.dto.request.SignInRequest;
import com.hibi.server.domain.auth.dto.request.SignUpRequest;
import com.hibi.server.domain.comment.dto.request.CommentCreateRequest;
import com.hibi.server.domain.feedpost.dto.request.FeedPostCreateRequest;
import com.hibi.server.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E 테스트: 게시글 작성 → 댓글 → 삭제 플로우
 */
@DisplayName("E2E: 게시글/댓글 플로우")
class PostCommentFlowE2ETest extends IntegrationTestSupport {

    private String accessToken;
    private Long memberId;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트용 회원 생성 및 로그인
        String email = "post-test@example.com";
        String password = "password123";

        SignUpRequest signUpRequest = new SignUpRequest(email, password, "게시글테스터");
        mockMvc.perform(post("/api/v1/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)));

        SignInRequest signInRequest = new SignInRequest(email, password);
        MvcResult signInResult = mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andReturn();

        String responseBody = signInResult.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(responseBody)
                .path("data").path("accessToken").asText();
        memberId = objectMapper.readTree(responseBody)
                .path("data").path("memberId").asLong();
    }

    @Test
    @DisplayName("게시글 작성 → 댓글 작성 → 대댓글 작성 → 게시글 삭제 플로우가 정상 동작한다")
    void postCommentFlow_create_comment_reply_delete() throws Exception {
        // === Step 1: 게시글 작성 ===
        FeedPostCreateRequest postRequest = new FeedPostCreateRequest(
                "오늘의 JPOP 추천입니다!",
                List.of("https://example.com/image1.jpg"),
                null
        );

        MvcResult postResult = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("오늘의 JPOP 추천입니다!"))
                .andReturn();

        Long postId = objectMapper.readTree(postResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // === Step 2: 댓글 작성 ===
        CommentCreateRequest commentRequest = new CommentCreateRequest("좋은 추천이네요!", null);

        MvcResult commentResult = mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("좋은 추천이네요!"))
                .andReturn();

        Long commentId = objectMapper.readTree(commentResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // === Step 3: 대댓글 작성 ===
        CommentCreateRequest replyRequest = new CommentCreateRequest("감사합니다!", commentId);

        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("감사합니다!"));

        // === Step 4: 댓글 목록 조회 ===
        mockMvc.perform(get("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(2));

        // === Step 5: 게시글 삭제 ===
        mockMvc.perform(delete("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Note: 삭제 후 조회 테스트는 MockMvc + @Transactional 환경에서
        // Hibernate 세션 충돌로 인해 별도 테스트로 분리
    }

    @Test
    @DisplayName("게시글 작성 → 좋아요 → 좋아요 취소 플로우가 정상 동작한다")
    void postFlow_create_like_unlike() throws Exception {
        // === Step 1: 게시글 작성 ===
        FeedPostCreateRequest postRequest = new FeedPostCreateRequest(
                "좋아요 테스트 게시글",
                List.of(),
                null
        );

        MvcResult postResult = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Long postId = objectMapper.readTree(postResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // === Step 2: 좋아요 추가 ===
        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true)); // isLiked = true

        // === Step 3: 게시글 조회하여 좋아요 수 확인 ===
        mockMvc.perform(get("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.isLiked").value(true));

        // === Step 4: 좋아요 취소 ===
        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false)); // isLiked = false

        // === Step 5: 좋아요 수 0 확인 ===
        mockMvc.perform(get("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.isLiked").value(false));
    }

    @Test
    @DisplayName("댓글 좋아요 → 댓글 삭제 플로우가 정상 동작한다")
    void commentFlow_like_delete() throws Exception {
        // 게시글 작성
        FeedPostCreateRequest postRequest = new FeedPostCreateRequest("댓글 테스트용", List.of(), null);
        MvcResult postResult = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andReturn();
        Long postId = objectMapper.readTree(postResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // === Step 1: 댓글 작성 ===
        CommentCreateRequest commentRequest = new CommentCreateRequest("테스트 댓글입니다", null);
        MvcResult commentResult = mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andReturn();
        Long commentId = objectMapper.readTree(commentResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // === Step 2: 댓글 좋아요 ===
        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments/" + commentId + "/like")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // === Step 3: 댓글 삭제 ===
        mockMvc.perform(delete("/api/v1/posts/" + postId + "/comments/" + commentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
