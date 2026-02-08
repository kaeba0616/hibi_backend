package com.hibi.server.domain.auth.service;

import com.hibi.server.domain.auth.dto.CustomUserDetails;
import com.hibi.server.domain.auth.dto.request.SignInRequest;
import com.hibi.server.domain.auth.dto.request.SignUpRequest;
import com.hibi.server.domain.auth.dto.response.SignInResponse;
import com.hibi.server.domain.auth.jwt.JwtUtils;
import com.hibi.server.domain.member.entity.Member;
import com.hibi.server.domain.member.entity.MemberStatus;
import com.hibi.server.domain.member.entity.ProviderType;
import com.hibi.server.domain.member.entity.UserRoleType;
import com.hibi.server.domain.member.repository.MemberRepository;
import com.hibi.server.domain.member.validator.MemberValidator;
import com.hibi.server.global.exception.CustomException;
import com.hibi.server.global.exception.ErrorCode;
import com.hibi.server.support.ServiceTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@DisplayName("AuthService 단위 테스트")
class AuthServiceTest extends ServiceTestSupport {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private MemberValidator memberValidator;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("signUp 메서드")
    class SignUpTest {

        @Test
        @DisplayName("신규 회원가입이 성공한다")
        void signUp_신규회원_성공() {
            // given
            SignUpRequest request = new SignUpRequest("user@example.com", "password1", "테스트유저");
            willDoNothing().given(memberValidator).validateEmail(anyString());
            willDoNothing().given(memberValidator).validatePassword(anyString());
            willDoNothing().given(memberValidator).validateNickname(anyString(), eq(null));
            given(memberRepository.findByEmailAndDeletedAtIsNotNull("user@example.com"))
                    .willReturn(Optional.empty());
            given(passwordEncoder.encode("password1")).willReturn("encodedPassword");

            // when
            authService.signUp(request);

            // then
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            then(memberRepository).should(times(1)).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getEmail()).isEqualTo("user@example.com");
            assertThat(savedMember.getPassword()).isEqualTo("encodedPassword");
            assertThat(savedMember.getNickname()).isEqualTo("테스트유저");
            assertThat(savedMember.getProvider()).isEqualTo(ProviderType.NATIVE);
            assertThat(savedMember.getRole()).isEqualTo(UserRoleType.USER);
        }

        @Test
        @DisplayName("삭제된 회원이 재가입하면 계정이 재활성화된다")
        void signUp_삭제된회원_재활성화() {
            // given
            SignUpRequest request = new SignUpRequest("user@example.com", "newPass1", "새닉네임");
            willDoNothing().given(memberValidator).validateEmail(anyString());
            willDoNothing().given(memberValidator).validatePassword(anyString());
            willDoNothing().given(memberValidator).validateNickname(anyString(), eq(null));

            Member deletedMember = Member.builder()
                    .email("user@example.com")
                    .password("oldEncodedPassword")
                    .nickname("이전닉네임")
                    .provider(ProviderType.NATIVE)
                    .role(UserRoleType.USER)
                    .status(MemberStatus.ACTIVE)
                    .build();
            deletedMember.softDelete(LocalDateTime.now());

            given(memberRepository.findByEmailAndDeletedAtIsNotNull("user@example.com"))
                    .willReturn(Optional.of(deletedMember));
            given(passwordEncoder.encode("newPass1")).willReturn("newEncodedPassword");

            // when
            authService.signUp(request);

            // then
            then(memberRepository).should(never()).save(any(Member.class));
            assertThat(deletedMember.getDeletedAt()).isNull();
            assertThat(deletedMember.getPassword()).isEqualTo("newEncodedPassword");
            assertThat(deletedMember.getNickname()).isEqualTo("새닉네임");
        }

        @Test
        @DisplayName("이메일이 중복이면 예외가 발생한다")
        void signUp_이메일중복_예외() {
            // given
            SignUpRequest request = new SignUpRequest("duplicate@example.com", "password1", "테스트유저");
            willThrow(new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS))
                    .given(memberValidator).validateEmail("duplicate@example.com");

            // when & then
            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
                    });

            then(memberRepository).should(never()).save(any(Member.class));
        }

        @Test
        @DisplayName("비밀번호 형식이 잘못되면 예외가 발생한다")
        void signUp_비밀번호형식오류_예외() {
            // given
            SignUpRequest request = new SignUpRequest("user@example.com", "weak", "테스트유저");
            willDoNothing().given(memberValidator).validateEmail(anyString());
            willThrow(new CustomException(ErrorCode.INVALID_PASSWORD_PATTERN))
                    .given(memberValidator).validatePassword("weak");

            // when & then
            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD_PATTERN);
                    });

            then(memberRepository).should(never()).save(any(Member.class));
        }

        @Test
        @DisplayName("닉네임이 중복이면 예외가 발생한다")
        void signUp_닉네임중복_예외() {
            // given
            SignUpRequest request = new SignUpRequest("user@example.com", "password1", "중복닉네임");
            willDoNothing().given(memberValidator).validateEmail(anyString());
            willDoNothing().given(memberValidator).validatePassword(anyString());
            willThrow(new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS))
                    .given(memberValidator).validateNickname("중복닉네임", null);

            // when & then
            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(ErrorCode.NICKNAME_ALREADY_EXISTS);
                    });

            then(memberRepository).should(never()).save(any(Member.class));
        }
    }

    @Nested
    @DisplayName("signIn 메서드")
    class SignInTest {

        @Test
        @DisplayName("올바른 자격증명으로 로그인이 성공한다")
        void signIn_성공() {
            // given
            SignInRequest request = new SignInRequest("user@example.com", "password1");

            Member member = Member.builder()
                    .id(1L)
                    .email("user@example.com")
                    .password("encodedPassword")
                    .nickname("테스트유저")
                    .provider(ProviderType.NATIVE)
                    .role(UserRoleType.USER)
                    .status(MemberStatus.ACTIVE)
                    .build();

            CustomUserDetails userDetails = new CustomUserDetails(member);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(authentication);
            given(jwtUtils.generateAccessToken(authentication)).willReturn("access-token");
            given(refreshTokenService.createAndSaveRefreshToken(eq(1L), eq(authentication)))
                    .willReturn("refresh-token");

            // when
            SignInResponse response = authService.signIn(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("잘못된 자격증명으로 로그인하면 예외가 발생한다")
        void signIn_잘못된자격증명_예외() {
            // given
            SignInRequest request = new SignInRequest("user@example.com", "wrongPassword");

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willThrow(new BadCredentialsException("Bad credentials"));

            // when & then
            assertThatThrownBy(() -> authService.signIn(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("signOut 메서드")
    class SignOutTest {

        @Test
        @DisplayName("로그아웃 시 모든 Refresh Token이 무효화된다")
        void signOut_성공() {
            // given
            Long memberId = 1L;
            willDoNothing().given(refreshTokenService).invalidateAllRefreshTokensForMember(memberId);

            // when
            authService.signOut(memberId);

            // then
            then(refreshTokenService).should(times(1))
                    .invalidateAllRefreshTokensForMember(memberId);
        }
    }

    @Nested
    @DisplayName("checkEmailAvailability 메서드")
    class CheckEmailTest {

        @Test
        @DisplayName("사용 가능한 이메일이면 예외가 발생하지 않는다")
        void checkEmail_사용가능_성공() {
            // given
            willDoNothing().given(memberValidator).validateEmail("new@example.com");

            // when & then (no exception)
            authService.checkEmailAvailability("new@example.com");

            then(memberValidator).should(times(1)).validateEmail("new@example.com");
        }

        @Test
        @DisplayName("이미 사용 중인 이메일이면 예외가 발생한다")
        void checkEmail_사용중_예외() {
            // given
            willThrow(new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS))
                    .given(memberValidator).validateEmail("existing@example.com");

            // when & then
            assertThatThrownBy(() -> authService.checkEmailAvailability("existing@example.com"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
                    });
        }
    }

    @Nested
    @DisplayName("checkNicknameAvailability 메서드")
    class CheckNicknameTest {

        @Test
        @DisplayName("사용 가능한 닉네임이면 예외가 발생하지 않는다")
        void checkNickname_사용가능_성공() {
            // given
            willDoNothing().given(memberValidator).validateNickname("새닉네임", null);

            // when & then (no exception)
            authService.checkNicknameAvailability("새닉네임");

            then(memberValidator).should(times(1)).validateNickname("새닉네임", null);
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 예외가 발생한다")
        void checkNickname_사용중_예외() {
            // given
            willThrow(new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS))
                    .given(memberValidator).validateNickname("기존닉네임", null);

            // when & then
            assertThatThrownBy(() -> authService.checkNicknameAvailability("기존닉네임"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(ErrorCode.NICKNAME_ALREADY_EXISTS);
                    });
        }
    }
}
