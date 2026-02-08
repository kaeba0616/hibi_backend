package com.hibi.server.domain.auth.service;

import com.hibi.server.domain.auth.entity.RefreshToken;
import com.hibi.server.domain.auth.jwt.JwtUtils;
import com.hibi.server.domain.auth.repository.RefreshTokenRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;

@DisplayName("RefreshTokenService 단위 테스트")
class RefreshTokenServiceTest extends ServiceTestSupport {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private Member createTestMember(Long id) {
        return Member.builder()
                .id(id)
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("테스트유저")
                .provider(ProviderType.NATIVE)
                .role(UserRoleType.USER)
                .status(MemberStatus.ACTIVE)
                .build();
    }

    private RefreshToken createActiveToken(Member member, String tokenValue) {
        return RefreshToken.of(
                member,
                tokenValue,
                LocalDateTime.now().plusDays(7),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("createAndSaveRefreshToken 메서드")
    class CreateAndSaveTest {

        @Test
        @DisplayName("새로운 Refresh Token이 생성되고 기존 토큰은 revoke된다")
        void createAndSave_성공() {
            // given
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            Authentication authentication = new UsernamePasswordAuthenticationToken("test@example.com", null);

            RefreshToken existingToken = createActiveToken(member, "old-token");
            given(refreshTokenRepository.findByMemberId(memberId))
                    .willReturn(List.of(existingToken));
            given(jwtUtils.generateRefreshToken(authentication)).willReturn("new-refresh-token");
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(jwtUtils.getRefreshTokenExpiryDate()).willReturn(LocalDateTime.now().plusDays(7));

            // when
            String result = refreshTokenService.createAndSaveRefreshToken(memberId, authentication);

            // then
            assertThat(result).isEqualTo("new-refresh-token");
            assertThat(existingToken.isRevoked()).isTrue();

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            then(refreshTokenRepository).should(times(1)).save(captor.capture());
            RefreshToken savedToken = captor.getValue();
            assertThat(savedToken.getTokenValue()).isEqualTo("new-refresh-token");
            assertThat(savedToken.getMember()).isEqualTo(member);
            assertThat(savedToken.isRevoked()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외가 발생한다")
        void createAndSave_회원없음_예외() {
            // given
            Long memberId = 999L;
            Authentication authentication = new UsernamePasswordAuthenticationToken("unknown@example.com", null);

            given(refreshTokenRepository.findByMemberId(memberId)).willReturn(Collections.emptyList());
            given(jwtUtils.generateRefreshToken(authentication)).willReturn("new-token");
            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> refreshTokenService.createAndSaveRefreshToken(memberId, authentication))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(ErrorCode.BAD_CREDENTIALS);
                    });
        }
    }

    @Nested
    @DisplayName("reissueTokens 메서드")
    class ReissueTokensTest {

        @Test
        @DisplayName("유효한 Refresh Token으로 토큰 재발급이 성공한다")
        void reissueTokens_성공() {
            // given
            String submittedToken = "valid-refresh-token";
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            RefreshToken currentToken = createActiveToken(member, submittedToken);

            willDoNothing().given(jwtUtils).validateJwtToken(submittedToken);
            given(jwtUtils.getMemberIdFromJwtToken(submittedToken)).willReturn(memberId);
            given(refreshTokenRepository.findByMemberIdAndTokenValueAndRevokedFalse(memberId, submittedToken))
                    .willReturn(Optional.of(currentToken));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(jwtUtils.generateAccessToken(any(Authentication.class))).willReturn("new-access-token");
            given(jwtUtils.generateRefreshToken(any(Authentication.class))).willReturn("new-refresh-token");

            // when
            var response = refreshTokenService.reissueTokens(submittedToken);

            // then
            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
            assertThat(currentToken.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("memberId가 추출되지 않으면 AUTHENTICATION_FAILED 예외가 발생한다")
        void reissueTokens_memberIdNull_예외() {
            // given
            String submittedToken = "token-without-memberId";
            willDoNothing().given(jwtUtils).validateJwtToken(submittedToken);
            given(jwtUtils.getMemberIdFromJwtToken(submittedToken)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> refreshTokenService.reissueTokens(submittedToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(ErrorCode.AUTHENTICATION_FAILED);
                    });
        }

        @Test
        @DisplayName("이전 토큰으로 재발급 시도하면 Replay Attack으로 감지된다")
        void reissueTokens_ReplayAttack_예외() {
            // given
            String submittedToken = "old-previous-token";
            Long memberId = 1L;
            Member member = createTestMember(memberId);

            RefreshToken activeToken = createActiveToken(member, "current-token");

            willDoNothing().given(jwtUtils).validateJwtToken(submittedToken);
            given(jwtUtils.getMemberIdFromJwtToken(submittedToken)).willReturn(memberId);
            given(refreshTokenRepository.findByMemberIdAndTokenValueAndRevokedFalse(memberId, submittedToken))
                    .willReturn(Optional.empty());
            given(refreshTokenRepository.findByMemberIdAndPreviousTokenValueAndRevokedFalse(memberId, submittedToken))
                    .willReturn(Optional.of(activeToken));
            given(refreshTokenRepository.findByMemberIdAndRevokedFalse(memberId))
                    .willReturn(List.of(activeToken));

            // when & then
            assertThatThrownBy(() -> refreshTokenService.reissueTokens(submittedToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(ErrorCode.REPLAY_ATTACK);
                    });

            // 모든 활성 토큰이 revoke되었는지 확인
            assertThat(activeToken.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("알 수 없는 토큰이면 JWT_INVALID_TOKEN 예외가 발생한다")
        void reissueTokens_알수없는토큰_예외() {
            // given
            String submittedToken = "unknown-token";
            Long memberId = 1L;

            willDoNothing().given(jwtUtils).validateJwtToken(submittedToken);
            given(jwtUtils.getMemberIdFromJwtToken(submittedToken)).willReturn(memberId);
            given(refreshTokenRepository.findByMemberIdAndTokenValueAndRevokedFalse(memberId, submittedToken))
                    .willReturn(Optional.empty());
            given(refreshTokenRepository.findByMemberIdAndPreviousTokenValueAndRevokedFalse(memberId, submittedToken))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> refreshTokenService.reissueTokens(submittedToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(ErrorCode.JWT_INVALID_TOKEN);
                    });
        }
    }

    @Nested
    @DisplayName("invalidateAllRefreshTokensForMember 메서드")
    class InvalidateAllTest {

        @Test
        @DisplayName("회원의 모든 활성 Refresh Token이 revoke된다")
        void invalidateAll_성공() {
            // given
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            RefreshToken token1 = createActiveToken(member, "token-1");
            RefreshToken token2 = createActiveToken(member, "token-2");

            given(refreshTokenRepository.findByMemberIdAndRevokedFalse(memberId))
                    .willReturn(List.of(token1, token2));

            // when
            refreshTokenService.invalidateAllRefreshTokensForMember(memberId);

            // then
            assertThat(token1.isRevoked()).isTrue();
            assertThat(token2.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("활성 토큰이 없으면 아무 동작도 하지 않는다")
        void invalidateAll_활성토큰없음() {
            // given
            Long memberId = 1L;
            given(refreshTokenRepository.findByMemberIdAndRevokedFalse(memberId))
                    .willReturn(Collections.emptyList());

            // when
            refreshTokenService.invalidateAllRefreshTokensForMember(memberId);

            // then (no exception, no side effects)
            then(refreshTokenRepository).should(times(1)).findByMemberIdAndRevokedFalse(memberId);
        }
    }
}
