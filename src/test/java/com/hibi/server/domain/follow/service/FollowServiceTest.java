package com.hibi.server.domain.follow.service;

import com.hibi.server.domain.feedpost.repository.FeedPostRepository;
import com.hibi.server.domain.follow.dto.response.FollowListResponse;
import com.hibi.server.domain.follow.dto.response.UserProfileResponse;
import com.hibi.server.domain.follow.entity.MemberFollow;
import com.hibi.server.domain.follow.repository.MemberFollowRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@DisplayName("FollowService 단위 테스트")
class FollowServiceTest extends ServiceTestSupport {

    @Mock
    private MemberFollowRepository memberFollowRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private FeedPostRepository feedPostRepository;

    @InjectMocks
    private FollowService followService;

    private Member createTestMember(Long id, MemberStatus status) {
        return Member.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .password("encodedPassword")
                .nickname("유저" + id)
                .provider(ProviderType.NATIVE)
                .role(UserRoleType.USER)
                .status(status)
                .build();
    }

    private Member createTestMember(Long id) {
        return createTestMember(id, MemberStatus.ACTIVE);
    }

    private MemberFollow createMemberFollow(Long id, Member follower, Member following) {
        return MemberFollow.builder()
                .id(id)
                .follower(follower)
                .following(following)
                .build();
    }

    @Nested
    @DisplayName("getUserProfile 메서드")
    class GetUserProfileTest {

        @Test
        @DisplayName("프로필 조회가 성공한다")
        void getUserProfile_성공() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            Member targetMember = createTestMember(targetUserId);

            given(memberRepository.findById(targetUserId)).willReturn(Optional.of(targetMember));
            given(feedPostRepository.findByMemberIdOrderByCreatedAtDesc(eq(targetUserId), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of()));
            given(memberFollowRepository.countFollowersByUserId(targetUserId)).willReturn(10L);
            given(memberFollowRepository.countFollowingsByUserId(targetUserId)).willReturn(5L);
            given(memberFollowRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId))
                    .willReturn(true);

            // when
            UserProfileResponse response = followService.getUserProfile(targetUserId, currentUserId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.nickname()).isEqualTo("유저1");
            assertThat(response.followerCount()).isEqualTo(10);
            assertThat(response.followingCount()).isEqualTo(5);
            assertThat(response.isFollowing()).isTrue();
        }

        @Test
        @DisplayName("삭제된 사용자 프로필 조회 시 예외가 발생한다")
        void getUserProfile_삭제된회원_예외() {
            // given
            Long targetUserId = 1L;
            Member deletedMember = createTestMember(targetUserId);
            deletedMember.softDelete(java.time.LocalDateTime.now());

            given(memberRepository.findById(targetUserId)).willReturn(Optional.of(deletedMember));

            // when & then
            assertThatThrownBy(() -> followService.getUserProfile(targetUserId, 2L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getFollowers 메서드")
    class GetFollowersTest {

        @Test
        @DisplayName("팔로워 목록 조회가 성공한다")
        void getFollowers_성공() {
            // given
            Long userId = 1L;
            Long currentUserId = 2L;
            Member user = createTestMember(userId);
            Member follower1 = createTestMember(3L);
            Member follower2 = createTestMember(4L);

            MemberFollow follow1 = createMemberFollow(1L, follower1, user);
            MemberFollow follow2 = createMemberFollow(2L, follower2, user);

            Page<MemberFollow> followersPage = new PageImpl<>(List.of(follow1, follow2));

            given(memberFollowRepository.findFollowersByUserId(eq(userId), any(Pageable.class)))
                    .willReturn(followersPage);
            given(memberFollowRepository.findFollowingIdsAmong(eq(currentUserId), any()))
                    .willReturn(List.of());

            // when
            FollowListResponse response = followService.getFollowers(userId, currentUserId, 0, 10);

            // then
            assertThat(response.content()).hasSize(2);
            assertThat(response.totalCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("follow 메서드")
    class FollowTest {

        @Test
        @DisplayName("팔로우가 성공한다")
        void follow_성공() {
            // given
            Long targetUserId = 2L;
            Long currentUserId = 1L;
            Member targetMember = createTestMember(targetUserId);
            Member currentMember = createTestMember(currentUserId);

            given(memberRepository.findById(targetUserId)).willReturn(Optional.of(targetMember));
            given(memberRepository.findById(currentUserId)).willReturn(Optional.of(currentMember));
            given(memberFollowRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId))
                    .willReturn(false);

            // when
            followService.follow(targetUserId, currentUserId);

            // then
            then(memberFollowRepository).should(times(1)).save(any(MemberFollow.class));
        }

        @Test
        @DisplayName("자기 자신 팔로우 시 예외가 발생한다")
        void follow_자기자신_예외() {
            // given
            Long userId = 1L;

            // when & then
            assertThatThrownBy(() -> followService.follow(userId, userId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            then(memberFollowRepository).should(never()).save(any(MemberFollow.class));
        }

        @Test
        @DisplayName("이미 팔로우 중이면 ALREADY_EXISTS 예외가 발생한다")
        void follow_이미팔로우_예외() {
            // given
            Long targetUserId = 2L;
            Long currentUserId = 1L;
            Member targetMember = createTestMember(targetUserId);
            Member currentMember = createTestMember(currentUserId);

            given(memberRepository.findById(targetUserId)).willReturn(Optional.of(targetMember));
            given(memberRepository.findById(currentUserId)).willReturn(Optional.of(currentMember));
            given(memberFollowRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> followService.follow(targetUserId, currentUserId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ALREADY_EXISTS));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 팔로우 시 예외가 발생한다")
        void follow_사용자없음_예외() {
            // given
            Long targetUserId = 999L;
            Long currentUserId = 1L;

            given(memberRepository.findById(targetUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> followService.follow(targetUserId, currentUserId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("unfollow 메서드")
    class UnfollowTest {

        @Test
        @DisplayName("언팔로우가 성공한다")
        void unfollow_성공() {
            // given
            Long targetUserId = 2L;
            Long currentUserId = 1L;
            Member targetMember = createTestMember(targetUserId);
            Member currentMember = createTestMember(currentUserId);
            MemberFollow follow = createMemberFollow(1L, currentMember, targetMember);

            given(memberFollowRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId))
                    .willReturn(Optional.of(follow));

            // when
            followService.unfollow(targetUserId, currentUserId);

            // then
            then(memberFollowRepository).should(times(1)).delete(follow);
        }

        @Test
        @DisplayName("자기 자신 언팔로우 시 예외가 발생한다")
        void unfollow_자기자신_예외() {
            // given
            Long userId = 1L;

            // when & then
            assertThatThrownBy(() -> followService.unfollow(userId, userId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        }

        @Test
        @DisplayName("팔로우 관계가 없으면 예외가 발생한다")
        void unfollow_관계없음_예외() {
            // given
            Long targetUserId = 2L;
            Long currentUserId = 1L;

            given(memberFollowRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> followService.unfollow(targetUserId, currentUserId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("isFollowing 메서드")
    class IsFollowingTest {

        @Test
        @DisplayName("팔로우 중이면 true를 반환한다")
        void isFollowing_팔로우중_true() {
            // given
            Long followerId = 1L;
            Long followingId = 2L;

            given(memberFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId))
                    .willReturn(true);

            // when
            boolean result = followService.isFollowing(followerId, followingId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("팔로우 중이 아니면 false를 반환한다")
        void isFollowing_팔로우안함_false() {
            // given
            Long followerId = 1L;
            Long followingId = 2L;

            given(memberFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId))
                    .willReturn(false);

            // when
            boolean result = followService.isFollowing(followerId, followingId);

            // then
            assertThat(result).isFalse();
        }
    }
}
