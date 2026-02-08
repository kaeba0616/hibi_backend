package com.hibi.server.domain.auth.controller;

import com.hibi.server.domain.auth.dto.request.SignInRequest;
import com.hibi.server.domain.auth.dto.request.SignUpRequest;
import com.hibi.server.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController 통합 테스트")
class AuthControllerIntegrationTest extends IntegrationTestSupport {

    @Nested
    @DisplayName("POST /api/v1/auth/sign-up")
    class SignUpTest {

        @Test
        @DisplayName("유효한 요청으로 회원가입이 성공한다")
        void signUp_성공() throws Exception {
            // given
            SignUpRequest request = new SignUpRequest("newuser@example.com", "password1", "새유저");

            // when & then
            mockMvc.perform(post("/api/v1/auth/sign-up")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("회원가입에 성공했습니다."));
        }

        @Test
        @DisplayName("이메일이 빈 값이면 400 에러가 반환된다")
        void signUp_이메일빈값_실패() throws Exception {
            // given
            SignUpRequest request = new SignUpRequest("", "password1", "테스트유저");

            // when & then
            mockMvc.perform(post("/api/v1/auth/sign-up")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("비밀번호가 형식에 맞지 않으면 400 에러가 반환된다")
        void signUp_비밀번호형식오류_실패() throws Exception {
            // given
            SignUpRequest request = new SignUpRequest("user@example.com", "weak", "테스트유저");

            // when & then
            mockMvc.perform(post("/api/v1/auth/sign-up")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("동일 이메일로 중복 가입하면 409 에러가 반환된다")
        void signUp_이메일중복_실패() throws Exception {
            // given - 첫 번째 가입
            SignUpRequest firstRequest = new SignUpRequest("dup@example.com", "password1", "유저1");
            mockMvc.perform(post("/api/v1/auth/sign-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstRequest)));

            // when - 동일 이메일로 재가입
            SignUpRequest duplicateRequest = new SignUpRequest("dup@example.com", "password2", "유저2");

            // then
            mockMvc.perform(post("/api/v1/auth/sign-up")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicateRequest)))
                    .andDo(print())
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/sign-in")
    class SignInTest {

        @Test
        @DisplayName("올바른 자격증명으로 로그인이 성공하고 토큰이 반환된다")
        void signIn_성공() throws Exception {
            // given - 먼저 회원가입
            SignUpRequest signUpRequest = new SignUpRequest("login@example.com", "password1", "로그인유저");
            mockMvc.perform(post("/api/v1/auth/sign-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpRequest)));

            // when
            SignInRequest signInRequest = new SignInRequest("login@example.com", "password1");

            // then
            mockMvc.perform(post("/api/v1/auth/sign-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signInRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists());
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인하면 401 에러가 반환된다")
        void signIn_잘못된비밀번호_실패() throws Exception {
            // given - 먼저 회원가입
            SignUpRequest signUpRequest = new SignUpRequest("wrong@example.com", "password1", "테스트유저2");
            mockMvc.perform(post("/api/v1/auth/sign-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpRequest)));

            // when & then
            SignInRequest signInRequest = new SignInRequest("wrong@example.com", "wrongPassword1");
            mockMvc.perform(post("/api/v1/auth/sign-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signInRequest)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/check-email")
    class CheckEmailTest {

        @Test
        @DisplayName("사용 가능한 이메일이면 성공 응답이 반환된다")
        void checkEmail_사용가능_성공() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/auth/check-email")
                            .param("email", "available@example.com"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("이미 사용 중인 이메일이면 409 에러가 반환된다")
        void checkEmail_사용중_실패() throws Exception {
            // given - 먼저 회원가입
            SignUpRequest signUpRequest = new SignUpRequest("taken@example.com", "password1", "이메일점유");
            mockMvc.perform(post("/api/v1/auth/sign-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpRequest)));

            // when & then
            mockMvc.perform(get("/api/v1/auth/check-email")
                            .param("email", "taken@example.com"))
                    .andDo(print())
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/check-nickname")
    class CheckNicknameTest {

        @Test
        @DisplayName("사용 가능한 닉네임이면 성공 응답이 반환된다")
        void checkNickname_사용가능_성공() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/auth/check-nickname")
                            .param("nickname", "새닉네임"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 409 에러가 반환된다")
        void checkNickname_사용중_실패() throws Exception {
            // given - 먼저 회원가입
            SignUpRequest signUpRequest = new SignUpRequest("nick@example.com", "password1", "점유닉네임");
            mockMvc.perform(post("/api/v1/auth/sign-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpRequest)));

            // when & then
            mockMvc.perform(get("/api/v1/auth/check-nickname")
                            .param("nickname", "점유닉네임"))
                    .andDo(print())
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("닉네임이 1글자이면 400 에러가 반환된다")
        void checkNickname_길이부족_실패() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/auth/check-nickname")
                            .param("nickname", "A"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }
}
