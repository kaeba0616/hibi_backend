package com.hibi.server.domain.comment.service;

import com.hibi.server.domain.comment.dto.request.CommentCreateRequest;
import com.hibi.server.domain.comment.dto.response.CommentResponse;
import com.hibi.server.domain.comment.entity.Comment;
import com.hibi.server.domain.comment.entity.CommentLike;
import com.hibi.server.domain.comment.repository.CommentLikeRepository;
import com.hibi.server.domain.comment.repository.CommentRepository;
import com.hibi.server.domain.feedpost.entity.FeedPost;
import com.hibi.server.domain.feedpost.repository.FeedPostRepository;
import com.hibi.server.domain.member.entity.Member;
import com.hibi.server.domain.member.entity.MemberStatus;
import com.hibi.server.domain.member.entity.ProviderType;
import com.hibi.server.domain.member.entity.UserRoleType;
import com.hibi.server.domain.member.repository.MemberRepository;
import com.hibi.server.global.exception.CustomException;
import com.hibi.server.global.exception.ErrorCode;
import com.hibi.server.support.ServiceTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@DisplayName("CommentService 단위 테스트")
class CommentServiceTest extends ServiceTestSupport {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private FeedPostRepository feedPostRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CommentService commentService;

    private Member createTestMember(Long id) {
        return Member.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .password("encodedPassword")
                .nickname("유저" + id)
                .provider(ProviderType.NATIVE)
                .role(UserRoleType.USER)
                .status(MemberStatus.ACTIVE)
                .build();
    }

    private FeedPost createTestPost(Long id, Member member) {
        return FeedPost.builder()
                .id(id)
                .member(member)
                .content("테스트 게시글")
                .likeCount(0)
                .commentCount(0)
                .build();
    }

    private Comment createTestComment(Long id, FeedPost feedPost, Member member) {
        return Comment.builder()
                .id(id)
                .feedPost(feedPost)
                .member(member)
                .content("테스트 댓글")
                .likeCount(0)
                .isDeleted(false)
                .build();
    }

    private Comment createTestReply(Long id, FeedPost feedPost, Member member, Comment parent) {
        return Comment.builder()
                .id(id)
                .feedPost(feedPost)
                .member(member)
                .content("테스트 대댓글")
                .parent(parent)
                .likeCount(0)
                .isDeleted(false)
                .build();
    }

    @Nested
    @DisplayName("createComment 메서드")
    class CreateCommentTest {

        @Test
        @DisplayName("일반 댓글 작성이 성공한다")
        void createComment_일반댓글_성공() {
            // given
            Long postId = 1L;
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            FeedPost feedPost = createTestPost(postId, member);
            CommentCreateRequest request = new CommentCreateRequest("새 댓글입니다", null);

            Comment savedComment = Comment.builder()
                    .id(1L)
                    .feedPost(feedPost)
                    .member(member)
                    .content("새 댓글입니다")
                    .likeCount(0)
                    .isDeleted(false)
                    .build();

            given(feedPostRepository.findById(postId)).willReturn(Optional.of(feedPost));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(commentRepository.save(any(Comment.class))).willReturn(savedComment);

            // when
            CommentResponse response = commentService.createComment(postId, request, memberId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.content()).isEqualTo("새 댓글입니다");
            assertThat(feedPost.getCommentCount()).isEqualTo(1);
            then(commentRepository).should(times(1)).save(any(Comment.class));
        }

        @Test
        @DisplayName("대댓글 작성이 성공한다")
        void createComment_대댓글_성공() {
            // given
            Long postId = 1L;
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            FeedPost feedPost = createTestPost(postId, member);

            Comment parentComment = createTestComment(10L, feedPost, member);

            CommentCreateRequest request = new CommentCreateRequest("대댓글입니다", 10L);

            Comment savedReply = Comment.builder()
                    .id(2L)
                    .feedPost(feedPost)
                    .member(member)
                    .content("대댓글입니다")
                    .parent(parentComment)
                    .likeCount(0)
                    .isDeleted(false)
                    .build();

            given(feedPostRepository.findById(postId)).willReturn(Optional.of(feedPost));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(commentRepository.findById(10L)).willReturn(Optional.of(parentComment));
            given(commentRepository.save(any(Comment.class))).willReturn(savedReply);

            // when
            CommentResponse response = commentService.createComment(postId, request, memberId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.content()).isEqualTo("대댓글입니다");
            assertThat(feedPost.getCommentCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("대대댓글 작성 시 INVALID_INPUT_VALUE 예외가 발생한다")
        void createComment_대대댓글_예외() {
            // given
            Long postId = 1L;
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            FeedPost feedPost = createTestPost(postId, member);

            Comment grandParent = createTestComment(10L, feedPost, member);
            Comment parent = createTestReply(11L, feedPost, member, grandParent);

            CommentCreateRequest request = new CommentCreateRequest("대대댓글 시도", 11L);

            given(feedPostRepository.findById(postId)).willReturn(Optional.of(feedPost));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(commentRepository.findById(11L)).willReturn(Optional.of(parent));

            // when & then
            assertThatThrownBy(() -> commentService.createComment(postId, request, memberId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            then(commentRepository).should(never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 예외가 발생한다")
        void createComment_게시글없음_예외() {
            // given
            CommentCreateRequest request = new CommentCreateRequest("댓글", null);
            given(feedPostRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.createComment(999L, request, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("deleteComment 메서드")
    class DeleteCommentTest {

        @Test
        @DisplayName("대댓글이 없는 댓글을 삭제하면 hard delete된다")
        void deleteComment_대댓글없음_hardDelete() {
            // given
            Long commentId = 1L;
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            FeedPost feedPost = createTestPost(1L, member);
            Comment comment = createTestComment(commentId, feedPost, member);

            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when
            commentService.deleteComment(commentId, memberId);

            // then
            then(commentLikeRepository).should(times(1)).deleteByCommentId(commentId);
            then(commentRepository).should(times(1)).delete(comment);
        }

        @Test
        @DisplayName("대댓글이 있는 댓글을 삭제하면 soft delete된다")
        void deleteComment_대댓글있음_softDelete() {
            // given
            Long commentId = 1L;
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            FeedPost feedPost = createTestPost(1L, member);

            Comment comment = Comment.builder()
                    .id(commentId)
                    .feedPost(feedPost)
                    .member(member)
                    .content("원본 댓글")
                    .likeCount(0)
                    .isDeleted(false)
                    .replies(List.of(createTestReply(2L, feedPost, member, null)))
                    .build();

            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when
            commentService.deleteComment(commentId, memberId);

            // then
            assertThat(comment.getIsDeleted()).isTrue();
            assertThat(comment.getContent()).isEmpty();
            then(commentRepository).should(never()).delete(any(Comment.class));
        }

        @Test
        @DisplayName("타인 댓글 삭제 시 UNAUTHORIZED_ACCESS 예외가 발생한다")
        void deleteComment_타인_예외() {
            // given
            Long commentId = 1L;
            Member author = createTestMember(1L);
            FeedPost feedPost = createTestPost(1L, author);
            Comment comment = createTestComment(commentId, feedPost, author);

            Long otherMemberId = 2L;

            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.deleteComment(commentId, otherMemberId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.UNAUTHORIZED_ACCESS));

            then(commentRepository).should(never()).delete(any(Comment.class));
        }
    }

    @Nested
    @DisplayName("toggleLike 메서드")
    class ToggleLikeTest {

        @Test
        @DisplayName("좋아요가 없으면 추가된다")
        void toggleLike_추가() {
            // given
            Long commentId = 1L;
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            FeedPost feedPost = createTestPost(1L, member);
            Comment comment = createTestComment(commentId, feedPost, member);

            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(commentLikeRepository.existsByMemberIdAndCommentId(memberId, commentId)).willReturn(false);

            // when
            boolean result = commentService.toggleLike(commentId, memberId);

            // then
            assertThat(result).isTrue();
            assertThat(comment.getLikeCount()).isEqualTo(1);
            then(commentLikeRepository).should(times(1)).save(any(CommentLike.class));
        }

        @Test
        @DisplayName("좋아요가 있으면 취소된다")
        void toggleLike_취소() {
            // given
            Long commentId = 1L;
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            FeedPost feedPost = createTestPost(1L, member);
            Comment comment = Comment.builder()
                    .id(commentId)
                    .feedPost(feedPost)
                    .member(member)
                    .content("테스트")
                    .likeCount(1)
                    .isDeleted(false)
                    .build();

            CommentLike existingLike = CommentLike.of(member, comment);

            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(commentLikeRepository.existsByMemberIdAndCommentId(memberId, commentId)).willReturn(true);
            given(commentLikeRepository.findByMemberIdAndCommentId(memberId, commentId))
                    .willReturn(Optional.of(existingLike));

            // when
            boolean result = commentService.toggleLike(commentId, memberId);

            // then
            assertThat(result).isFalse();
            assertThat(comment.getLikeCount()).isEqualTo(0);
            then(commentLikeRepository).should(times(1)).delete(existingLike);
        }

        @Test
        @DisplayName("삭제된 댓글에 좋아요하면 INVALID_INPUT_VALUE 예외가 발생한다")
        void toggleLike_삭제된댓글_예외() {
            // given
            Long commentId = 1L;
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            FeedPost feedPost = createTestPost(1L, member);
            Comment deletedComment = Comment.builder()
                    .id(commentId)
                    .feedPost(feedPost)
                    .member(member)
                    .content("")
                    .likeCount(0)
                    .isDeleted(true)
                    .build();

            given(commentRepository.findById(commentId)).willReturn(Optional.of(deletedComment));

            // when & then
            assertThatThrownBy(() -> commentService.toggleLike(commentId, memberId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            then(commentLikeRepository).should(never()).save(any(CommentLike.class));
        }
    }
}
