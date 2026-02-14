package com.hibi.server.e2e;

import com.hibi.server.domain.auth.dto.request.SignInRequest;
import com.hibi.server.domain.auth.dto.request.SignUpRequest;
import com.hibi.server.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E 테스트: 회원가입 → 로그인 → 프로필 확인 플로우
 */
@DisplayName("E2E: 인증 플로우")
class AuthFlowE2ETest extends IntegrationTestSupport {

    @Test
    @DisplayName("회원가입 → 로그인 → 프로필 조회 전체 플로우가 정상 동작한다")
    void authFlow_signUp_signIn_getProfile() throws Exception {
        // === Step 1: 회원가입 ===
        String email = "e2e-test@example.com";
        String password = "password123";
        String nickname = "E2E테스터";

        SignUpRequest signUpRequest = new SignUpRequest(email, password, nickname);

        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입에 성공했습니다."));

        // === Step 2: 로그인 ===
        SignInRequest signInRequest = new SignInRequest(email, password);

        MvcResult signInResult = mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.memberId").exists())
                .andReturn();

        // 토큰 및 회원 ID 추출
        String responseBody = signInResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody)
                .path("data").path("accessToken").asText();
        Long memberId = objectMapper.readTree(responseBody)
                .path("data").path("memberId").asLong();

        // === Step 3: 내 프로필 조회 ===
        mockMvc.perform(get("/api/v1/members/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.nickname").value(nickname));
    }

    @Test
    @DisplayName("이메일 중복 확인 → 닉네임 중복 확인 → 회원가입 플로우가 정상 동작한다")
    void authFlow_checkDuplicates_signUp() throws Exception {
        String email = "unique@example.com";
        String nickname = "유니크닉네임";

        // === Step 1: 이메일 중복 확인 (사용 가능) ===
        mockMvc.perform(get("/api/v1/auth/check-email")
                        .param("email", email))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // === Step 2: 닉네임 중복 확인 (사용 가능) ===
        mockMvc.perform(get("/api/v1/auth/check-nickname")
                        .param("nickname", nickname))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // === Step 3: 회원가입 ===
        SignUpRequest signUpRequest = new SignUpRequest(email, "password123", nickname);

        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // === Step 4: 이메일 중복 확인 (사용 불가) ===
        mockMvc.perform(get("/api/v1/auth/check-email")
                        .param("email", email))
                .andDo(print())
                .andExpect(status().isConflict());

        // === Step 5: 닉네임 중복 확인 (사용 불가) ===
        mockMvc.perform(get("/api/v1/auth/check-nickname")
                        .param("nickname", nickname))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("로그인 실패 → 재시도 → 로그인 성공 플로우가 정상 동작한다")
    void authFlow_loginFail_retry_success() throws Exception {
        // 먼저 회원가입
        String email = "retry-test@example.com";
        String password = "correctPassword1";

        SignUpRequest signUpRequest = new SignUpRequest(email, password, "재시도유저");
        mockMvc.perform(post("/api/v1/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)));

        // === Step 1: 잘못된 비밀번호로 로그인 시도 ===
        SignInRequest wrongRequest = new SignInRequest(email, "wrongPassword1");

        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // === Step 2: 올바른 비밀번호로 재시도 ===
        SignInRequest correctRequest = new SignInRequest(email, password);

        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists());
    }
}
