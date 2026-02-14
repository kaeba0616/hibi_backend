package com.hibi.server.e2e;

import com.hibi.server.domain.admin.dto.request.ReportActionRequest;
import com.hibi.server.domain.auth.dto.request.SignInRequest;
import com.hibi.server.domain.auth.dto.request.SignUpRequest;
import com.hibi.server.domain.feedpost.dto.request.FeedPostCreateRequest;
import com.hibi.server.domain.member.entity.Member;
import com.hibi.server.domain.member.entity.MemberStatus;
import com.hibi.server.domain.member.entity.ProviderType;
import com.hibi.server.domain.member.entity.UserRoleType;
import com.hibi.server.domain.member.repository.MemberRepository;
import com.hibi.server.domain.report.dto.request.ReportCreateRequest;
import com.hibi.server.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E 테스트: 신고 접수 → 관리자 처리 플로우
 */
@DisplayName("E2E: 신고/관리자 처리 플로우")
class ReportFlowE2ETest extends IntegrationTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authorAccessToken;  // 게시글 작성자
    private String reporterAccessToken; // 신고자
    private String adminAccessToken;
    private Long authorId;
    private Long reporterId;
    private Long adminId;

    @BeforeEach
    void setUp() throws Exception {
        // === 게시글 작성자 생성 및 로그인 ===
        String authorEmail = "author@example.com";
        String authorPassword = "password123";

        SignUpRequest authorSignUp = new SignUpRequest(authorEmail, authorPassword, "작성자");
        mockMvc.perform(post("/api/v1/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authorSignUp)));

        SignInRequest authorSignIn = new SignInRequest(authorEmail, authorPassword);
        MvcResult authorResult = mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorSignIn)))
                .andReturn();

        String authorResponse = authorResult.getResponse().getContentAsString();
        authorAccessToken = objectMapper.readTree(authorResponse)
                .path("data").path("accessToken").asText();
        authorId = objectMapper.readTree(authorResponse)
                .path("data").path("memberId").asLong();

        // === 신고자 생성 및 로그인 ===
        String reporterEmail = "reporter@example.com";
        String reporterPassword = "password123";

        SignUpRequest reporterSignUp = new SignUpRequest(reporterEmail, reporterPassword, "신고자");
        mockMvc.perform(post("/api/v1/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reporterSignUp)));

        SignInRequest reporterSignIn = new SignInRequest(reporterEmail, reporterPassword);
        MvcResult reporterResult = mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reporterSignIn)))
                .andReturn();

        String reporterResponse = reporterResult.getResponse().getContentAsString();
        reporterAccessToken = objectMapper.readTree(reporterResponse)
                .path("data").path("accessToken").asText();
        reporterId = objectMapper.readTree(reporterResponse)
                .path("data").path("memberId").asLong();

        // === 관리자 계정 직접 생성 (DB에 직접 ADMIN 역할로) ===
        String adminEmail = "admin@example.com";
        String adminPassword = "adminPassword123";

        Member adminMember = Member.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .nickname("관리자")
                .provider(ProviderType.NATIVE)
                .role(UserRoleType.ADMIN)
                .status(MemberStatus.ACTIVE)
                .build();
        memberRepository.save(adminMember);

        SignInRequest adminSignIn = new SignInRequest(adminEmail, adminPassword);
        MvcResult adminResult = mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminSignIn)))
                .andReturn();

        String adminResponse = adminResult.getResponse().getContentAsString();
        adminAccessToken = objectMapper.readTree(adminResponse)
                .path("data").path("accessToken").asText();
        adminId = objectMapper.readTree(adminResponse)
                .path("data").path("memberId").asLong();
    }

    @Test
    @DisplayName("게시글 신고 → 관리자 신고 목록 조회 → 신고 처리(기각) 플로우가 정상 동작한다")
    void reportFlow_createReport_adminDismiss() throws Exception {
        // === Step 1: 게시글 작성 (신고 대상) ===
        FeedPostCreateRequest postRequest = new FeedPostCreateRequest(
                "신고 테스트용 게시글",
                List.of(),
                null
        );

        MvcResult postResult = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + authorAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Long postId = objectMapper.readTree(postResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // === Step 2: 게시글 신고 ===
        ReportCreateRequest reportRequest = new ReportCreateRequest(
                "POST",
                postId,
                "SPAM",
                "스팸성 게시글입니다"
        );

        MvcResult reportResult = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("신고가 접수되었습니다"))
                .andReturn();

        Long reportId = objectMapper.readTree(reportResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // === Step 3: 관리자 - 신고 목록 조회 ===
        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(1));

        // === Step 4: 관리자 - 신고 상세 조회 ===
        mockMvc.perform(get("/api/v1/admin/reports/" + reportId)
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targetType").value("POST"))
                .andExpect(jsonPath("$.data.reason").value("SPAM"));

        // === Step 5: 관리자 - 신고 기각 처리 ===
        ReportActionRequest actionRequest = new ReportActionRequest(
                reportId,
                "DISMISS",
                "허위 신고로 판단됨",
                null
        );

        mockMvc.perform(post("/api/v1/admin/reports/process")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actionRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("신고 처리 완료"));

        // === Step 6: 처리 완료 후 신고 상세 확인 ===
        mockMvc.perform(get("/api/v1/admin/reports/" + reportId)
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISMISSED"));
    }

    @Test
    @DisplayName("게시글 신고 → 관리자 콘텐츠 삭제 처리 플로우가 정상 동작한다")
    void reportFlow_createReport_adminDeleteContent() throws Exception {
        // === Step 1: 게시글 작성 ===
        FeedPostCreateRequest postRequest = new FeedPostCreateRequest(
                "부적절한 콘텐츠 게시글",
                List.of(),
                null
        );

        MvcResult postResult = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + authorAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Long postId = objectMapper.readTree(postResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // === Step 2: 게시글 신고 ===
        ReportCreateRequest reportRequest = new ReportCreateRequest(
                "POST",
                postId,
                "INAPPROPRIATE",
                "부적절한 콘텐츠입니다"
        );

        MvcResult reportResult = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Long reportId = objectMapper.readTree(reportResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // === Step 3: 관리자 - 콘텐츠 삭제 처리 ===
        ReportActionRequest actionRequest = new ReportActionRequest(
                reportId,
                "DELETE_CONTENT",
                "커뮤니티 가이드라인 위반",
                null
        );

        mockMvc.perform(post("/api/v1/admin/reports/process")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actionRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // === Step 4: 처리 완료 후 신고 상태 확인 ===
        mockMvc.perform(get("/api/v1/admin/reports/" + reportId)
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        // === Step 5: 삭제된 게시글 조회 시도 → 실패 ===
        mockMvc.perform(get("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + authorAccessToken))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("중복 신고 시도 시 실패한다")
    void reportFlow_duplicateReport_fails() throws Exception {
        // === Step 1: 게시글 작성 ===
        FeedPostCreateRequest postRequest = new FeedPostCreateRequest(
                "중복 신고 테스트 게시글",
                List.of(),
                null
        );

        MvcResult postResult = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + authorAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Long postId = objectMapper.readTree(postResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // === Step 2: 첫 번째 신고 ===
        ReportCreateRequest reportRequest = new ReportCreateRequest(
                "POST",
                postId,
                "SPAM",
                null
        );

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andExpect(status().isOk());

        // === Step 3: 중복 신고 여부 확인 ===
        mockMvc.perform(get("/api/v1/reports/check")
                        .header("Authorization", "Bearer " + reporterAccessToken)
                        .param("targetType", "POST")
                        .param("targetId", postId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alreadyReported").value(true));

        // === Step 4: 중복 신고 시도 → 실패 ===
        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("일반 사용자가 관리자 API 접근 시 권한 오류가 발생한다")
    void reportFlow_userAccessAdminApi_forbidden() throws Exception {
        // 일반 사용자가 관리자 API에 접근 시도
        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + authorAccessToken))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
