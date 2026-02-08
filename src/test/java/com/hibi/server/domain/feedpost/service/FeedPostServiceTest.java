package com.hibi.server.domain.feedpost.service;

import com.hibi.server.domain.feedpost.dto.request.FeedPostCreateRequest;
import com.hibi.server.domain.feedpost.dto.request.FeedPostUpdateRequest;
import com.hibi.server.domain.feedpost.dto.response.FeedPostResponse;
import com.hibi.server.domain.feedpost.entity.FeedPost;
import com.hibi.server.domain.feedpost.entity.FeedPostLike;
import com.hibi.server.domain.feedpost.repository.FeedPostLikeRepository;
import com.hibi.server.domain.feedpost.repository.FeedPostRepository;
import com.hibi.server.domain.member.entity.Member;
import com.hibi.server.domain.member.entity.MemberStatus;
import com.hibi.server.domain.member.entity.ProviderType;
import com.hibi.server.domain.member.entity.UserRoleType;
import com.hibi.server.domain.member.repository.MemberRepository;
import com.hibi.server.domain.song.repository.SongRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@DisplayName("FeedPostService 단위 테스트")
class FeedPostServiceTest extends ServiceTestSupport {

    @Mock
    private FeedPostRepository feedPostRepository;

    @Mock
    private FeedPostLikeRepository feedPostLikeRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SongRepository songRepository;

    @InjectMocks
    private FeedPostService feedPostService;

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
                .content("테스트 게시글 내용")
                .likeCount(0)
                .commentCount(0)
                .build();
    }

    @Nested
    @DisplayName("createPost 메서드")
    class CreatePostTest {

        @Test
        @DisplayName("게시글 작성이 성공한다")
        void createPost_성공() {
            // given
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            FeedPostCreateRequest request = new FeedPostCreateRequest(
                    "오늘의 JPOP 추천!", List.of("https://img.example.com/1.jpg"), null
            );

            FeedPost savedPost = FeedPost.builder()
                    .id(1L)
                    .member(member)
                    .content("오늘의 JPOP 추천!")
                    .likeCount(0)
                    .commentCount(0)
                    .build();

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(feedPostRepository.save(any(FeedPost.class))).willReturn(savedPost);

            // when
            FeedPostResponse response = feedPostService.createPost(request, memberId);

            // then
            assertThat(response).isNotNull();
            then(feedPostRepository).should(times(1)).save(any(FeedPost.class));
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외가 발생한다")
        void createPost_회원없음_예외() {
            // given
            FeedPostCreateRequest request = new FeedPostCreateRequest("내용", List.of(), null);
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedPostService.createPost(request, 999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getPost 메서드")
    class GetPostTest {

        @Test
        @DisplayName("게시글 상세 조회가 성공한다")
        void getPost_성공() {
            // given
            Long postId = 1L;
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            FeedPost feedPost = createTestPost(postId, member);

            given(feedPostRepository.findWithDetailsById(postId)).willReturn(Optional.of(feedPost));
            given(feedPostLikeRepository.existsByMemberIdAndFeedPostId(memberId, postId)).willReturn(false);

            // when
            FeedPostResponse response = feedPostService.getPost(postId, memberId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isLiked()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 예외가 발생한다")
        void getPost_게시글없음_예외() {
            // given
            given(feedPostRepository.findWithDetailsById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedPostService.getPost(999L, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("updatePost 메서드")
    class UpdatePostTest {

        @Test
        @DisplayName("본인 게시글 수정이 성공한다")
        void updatePost_본인_성공() {
            // given
            Long postId = 1L;
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            FeedPost feedPost = createTestPost(postId, member);

            FeedPostUpdateRequest request = new FeedPostUpdateRequest("수정된 내용", List.of(), null);

            given(feedPostRepository.findWithDetailsById(postId)).willReturn(Optional.of(feedPost));
            given(feedPostLikeRepository.existsByMemberIdAndFeedPostId(memberId, postId)).willReturn(false);

            // when
            FeedPostResponse response = feedPostService.updatePost(postId, request, memberId);

            // then
            assertThat(response).isNotNull();
            assertThat(feedPost.getContent()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("타인 게시글 수정 시 UNAUTHORIZED_ACCESS 예외가 발생한다")
        void updatePost_타인_예외() {
            // given
            Long postId = 1L;
            Member author = createTestMember(1L);
            FeedPost feedPost = createTestPost(postId, author);

            Long otherMemberId = 2L;
            FeedPostUpdateRequest request = new FeedPostUpdateRequest("수정 시도", List.of(), null);

            given(feedPostRepository.findWithDetailsById(postId)).willReturn(Optional.of(feedPost));

            // when & then
            assertThatThrownBy(() -> feedPostService.updatePost(postId, request, otherMemberId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.UNAUTHORIZED_ACCESS));
        }
    }

    @Nested
    @DisplayName("deletePost 메서드")
    class DeletePostTest {

        @Test
        @DisplayName("본인 게시글 삭제가 성공한다")
        void deletePost_본인_성공() {
            // given
            Long postId = 1L;
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            FeedPost feedPost = createTestPost(postId, member);

            given(feedPostRepository.findById(postId)).willReturn(Optional.of(feedPost));

            // when
            feedPostService.deletePost(postId, memberId);

            // then
            then(feedPostRepository).should(times(1)).delete(feedPost);
        }

        @Test
        @DisplayName("타인 게시글 삭제 시 UNAUTHORIZED_ACCESS 예외가 발생한다")
        void deletePost_타인_예외() {
            // given
            Long postId = 1L;
            Member author = createTestMember(1L);
            FeedPost feedPost = createTestPost(postId, author);

            given(feedPostRepository.findById(postId)).willReturn(Optional.of(feedPost));

            // when & then
            assertThatThrownBy(() -> feedPostService.deletePost(postId, 2L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.UNAUTHORIZED_ACCESS));

            then(feedPostRepository).should(never()).delete(any(FeedPost.class));
        }
    }

    @Nested
    @DisplayName("toggleLike 메서드")
    class ToggleLikeTest {

        @Test
        @DisplayName("좋아요가 없으면 추가된다")
        void toggleLike_추가() {
            // given
            Long postId = 1L;
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            FeedPost feedPost = createTestPost(postId, member);

            given(feedPostRepository.findById(postId)).willReturn(Optional.of(feedPost));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(feedPostLikeRepository.existsByMemberIdAndFeedPostId(memberId, postId)).willReturn(false);

            // when
            boolean result = feedPostService.toggleLike(postId, memberId);

            // then
            assertThat(result).isTrue();
            assertThat(feedPost.getLikeCount()).isEqualTo(1);
            then(feedPostLikeRepository).should(times(1)).save(any(FeedPostLike.class));
        }

        @Test
        @DisplayName("좋아요가 있으면 취소된다")
        void toggleLike_취소() {
            // given
            Long postId = 1L;
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            FeedPost feedPost = FeedPost.builder()
                    .id(postId)
                    .member(member)
                    .content("테스트")
                    .likeCount(1)
                    .commentCount(0)
                    .build();

            given(feedPostRepository.findById(postId)).willReturn(Optional.of(feedPost));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(feedPostLikeRepository.existsByMemberIdAndFeedPostId(memberId, postId)).willReturn(true);

            // when
            boolean result = feedPostService.toggleLike(postId, memberId);

            // then
            assertThat(result).isFalse();
            assertThat(feedPost.getLikeCount()).isEqualTo(0);
            then(feedPostLikeRepository).should(times(1))
                    .deleteByMemberIdAndFeedPostId(memberId, postId);
        }
    }
}
