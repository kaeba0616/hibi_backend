package com.hibi.server.domain.admin.controller;

import com.hibi.server.domain.admin.dto.request.*;
import com.hibi.server.domain.faq.entity.FAQ;
import com.hibi.server.domain.faq.entity.FAQCategory;
import com.hibi.server.domain.faq.repository.FAQRepository;
import com.hibi.server.domain.member.entity.Member;
import com.hibi.server.domain.member.entity.MemberStatus;
import com.hibi.server.domain.member.entity.ProviderType;
import com.hibi.server.domain.member.entity.UserRoleType;
import com.hibi.server.domain.member.repository.MemberRepository;
import com.hibi.server.domain.question.entity.Question;
import com.hibi.server.domain.question.entity.QuestionStatus;
import com.hibi.server.domain.question.entity.QuestionType;
import com.hibi.server.domain.question.repository.QuestionRepository;
import com.hibi.server.domain.report.entity.Report;
import com.hibi.server.domain.report.entity.ReportReason;
import com.hibi.server.domain.report.entity.ReportStatus;
import com.hibi.server.domain.report.entity.ReportTargetType;
import com.hibi.server.domain.report.repository.ReportRepository;
import com.hibi.server.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AdminController 통합 테스트")
class AdminControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private FAQRepository faqRepository;

    private Member admin;
    private Member user;

    @BeforeEach
    void setUp() {
        // 관리자 생성
        admin = Member.builder()
                .email("admin@example.com")
                .password("encodedPassword")
                .nickname("관리자")
                .provider(ProviderType.NATIVE)
                .role(UserRoleType.ADMIN)
                .status(MemberStatus.ACTIVE)
                .build();
        memberRepository.save(admin);

        // 일반 유저 생성
        user = Member.builder()
                .email("user@example.com")
                .password("encodedPassword")
                .nickname("일반유저")
                .provider(ProviderType.NATIVE)
                .role(UserRoleType.USER)
                .status(MemberStatus.ACTIVE)
                .build();
        memberRepository.save(user);
    }

    @Nested
    @DisplayName("GET /api/v1/admin/stats")
    class GetStatsTest {

        @Test
        @DisplayName("관리자는 대시보드 통계를 조회할 수 있다")
        @WithMockUser(roles = "ADMIN")
        void getStats_관리자_성공() throws Exception {
            mockMvc.perform(get("/api/v1/admin/stats"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalMembers").exists())
                    .andExpect(jsonPath("$.data.pendingReports").exists());
        }

        @Test
        @DisplayName("일반 유저는 대시보드에 접근할 수 없다")
        @WithMockUser(roles = "USER")
        void getStats_일반유저_실패() throws Exception {
            mockMvc.perform(get("/api/v1/admin/stats"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/members")
    class GetMembersTest {

        @Test
        @DisplayName("관리자는 회원 목록을 조회할 수 있다")
        @WithMockUser(roles = "ADMIN")
        void getMembers_성공() throws Exception {
            mockMvc.perform(get("/api/v1/admin/members")
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.members").isArray());
        }

        @Test
        @DisplayName("상태별로 회원을 필터링할 수 있다")
        @WithMockUser(roles = "ADMIN")
        void getMembers_상태필터_성공() throws Exception {
            mockMvc.perform(get("/api/v1/admin/members")
                            .param("status", "ACTIVE")
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/members/sanction")
    class SanctionMemberTest {

        @Test
        @DisplayName("일반 유저는 회원을 제재할 수 없다")
        @WithMockUser(roles = "USER")
        void sanctionMember_일반유저_실패() throws Exception {
            MemberSanctionRequest request = new MemberSanctionRequest(
                    user.getId(), "SUSPEND", 7, "테스트 제재"
            );

            mockMvc.perform(post("/api/v1/admin/members/sanction")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/reports")
    class GetReportsTest {

        @Test
        @DisplayName("관리자는 신고 목록을 조회할 수 있다")
        @WithMockUser(roles = "ADMIN")
        void getReports_성공() throws Exception {
            // given - 신고 생성
            Report report = Report.builder()
                    .reporter(user)
                    .targetType(ReportTargetType.MEMBER)
                    .targetId(admin.getId())
                    .reason(ReportReason.SPAM)
                    .status(ReportStatus.PENDING)
                    .build();
            reportRepository.save(report);

            // when & then
            mockMvc.perform(get("/api/v1/admin/reports")
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.reports").isArray());
        }

        @Test
        @DisplayName("상태별로 신고를 필터링할 수 있다")
        @WithMockUser(roles = "ADMIN")
        void getReports_상태필터_성공() throws Exception {
            mockMvc.perform(get("/api/v1/admin/reports")
                            .param("status", "PENDING")
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/questions")
    class GetQuestionsTest {

        @Test
        @DisplayName("관리자는 문의 목록을 조회할 수 있다")
        @WithMockUser(roles = "ADMIN")
        void getQuestions_성공() throws Exception {
            // given - 문의 생성
            Question question = Question.builder()
                    .member(user)
                    .type(QuestionType.SERVICE)
                    .title("테스트 문의")
                    .content("문의 내용입니다")
                    .status(QuestionStatus.RECEIVED)
                    .build();
            questionRepository.save(question);

            // when & then
            mockMvc.perform(get("/api/v1/admin/questions")
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.questions").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/questions/answer")
    class AnswerQuestionTest {

        @Test
        @DisplayName("일반 유저는 문의에 답변할 수 없다")
        @WithMockUser(roles = "USER")
        void answerQuestion_일반유저_실패() throws Exception {
            QuestionAnswerRequest request = new QuestionAnswerRequest(1L, "답변입니다");

            mockMvc.perform(post("/api/v1/admin/questions/answer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/faqs")
    class GetFaqsTest {

        @Test
        @DisplayName("관리자는 FAQ 목록을 조회할 수 있다")
        @WithMockUser(roles = "ADMIN")
        void getFaqs_성공() throws Exception {
            // given - FAQ 생성
            FAQ faq = FAQ.builder()
                    .question("테스트 질문")
                    .answer("테스트 답변")
                    .category(FAQCategory.SERVICE)
                    .displayOrder(1)
                    .isPublished(true)
                    .build();
            faqRepository.save(faq);

            // when & then
            mockMvc.perform(get("/api/v1/admin/faqs"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.faqs").isArray());
        }

        @Test
        @DisplayName("카테고리별로 FAQ를 필터링할 수 있다")
        @WithMockUser(roles = "ADMIN")
        void getFaqs_카테고리필터_성공() throws Exception {
            mockMvc.perform(get("/api/v1/admin/faqs")
                            .param("category", "SERVICE"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/faqs")
    class SaveFaqTest {

        @Test
        @DisplayName("일반 유저는 FAQ를 생성할 수 없다")
        @WithMockUser(roles = "USER")
        void saveFaq_일반유저_실패() throws Exception {
            FAQSaveRequest request = new FAQSaveRequest(
                    null, "SERVICE", "새 질문", "새 답변", 1, true
            );

            mockMvc.perform(post("/api/v1/admin/faqs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/faqs/{faqId}")
    class DeleteFaqTest {

        @Test
        @DisplayName("일반 유저는 FAQ를 삭제할 수 없다")
        @WithMockUser(roles = "USER")
        void deleteFaq_일반유저_실패() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/faqs/1"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }
}
