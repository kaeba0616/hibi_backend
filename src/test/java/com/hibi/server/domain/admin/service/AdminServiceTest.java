package com.hibi.server.domain.admin.service;

import com.hibi.server.domain.admin.dto.request.*;
import com.hibi.server.domain.admin.dto.response.*;
import com.hibi.server.domain.comment.repository.CommentRepository;
import com.hibi.server.domain.faq.entity.FAQ;
import com.hibi.server.domain.faq.entity.FAQCategory;
import com.hibi.server.domain.faq.repository.FAQRepository;
import com.hibi.server.domain.feedpost.entity.FeedPost;
import com.hibi.server.domain.feedpost.repository.FeedPostRepository;
import com.hibi.server.domain.follow.repository.MemberFollowRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@DisplayName("AdminService 단위 테스트")
class AdminServiceTest extends ServiceTestSupport {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private FAQRepository faqRepository;

    @Mock
    private FeedPostRepository feedPostRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MemberFollowRepository memberFollowRepository;

    @InjectMocks
    private AdminService adminService;

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

    private Member createAdminMember(Long id) {
        return Member.builder()
                .id(id)
                .email("admin@example.com")
                .password("encodedPassword")
                .nickname("관리자")
                .provider(ProviderType.NATIVE)
                .role(UserRoleType.ADMIN)
                .status(MemberStatus.ACTIVE)
                .build();
    }

    private Report createTestReport(Long id, Member reporter, ReportTargetType targetType, Long targetId) {
        return Report.builder()
                .id(id)
                .reporter(reporter)
                .targetType(targetType)
                .targetId(targetId)
                .reason(ReportReason.SPAM)
                .description("테스트 신고")
                .status(ReportStatus.PENDING)
                .build();
    }

    private Question createTestQuestion(Long id, Member member) {
        return Question.builder()
                .id(id)
                .member(member)
                .type(QuestionType.SERVICE)
                .title("테스트 문의")
                .content("문의 내용입니다")
                .status(QuestionStatus.RECEIVED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private FAQ createTestFaq(Long id, FAQCategory category) {
        return FAQ.builder()
                .id(id)
                .question("자주 묻는 질문")
                .answer("답변입니다")
                .category(category)
                .displayOrder(1)
                .isPublished(true)
                .build();
    }

    @Nested
    @DisplayName("getStats 메서드")
    class GetStatsTest {

        @Test
        @DisplayName("대시보드 통계를 정상적으로 조회한다")
        void getStats_성공() {
            // given
            given(memberRepository.countByDeletedAtIsNull()).willReturn(100L);
            given(memberRepository.countTodayNewMembers()).willReturn(5L);
            given(reportRepository.countByStatus(ReportStatus.PENDING)).willReturn(10L);
            given(reportRepository.countTodayReports()).willReturn(3L);
            given(questionRepository.countUnansweredQuestions()).willReturn(7L);
            given(faqRepository.count()).willReturn(20L);

            // when
            AdminStatsResponse response = adminService.getStats();

            // then
            assertThat(response.totalMembers()).isEqualTo(100L);
            assertThat(response.todayNewMembers()).isEqualTo(5L);
            assertThat(response.pendingReports()).isEqualTo(10L);
            assertThat(response.todayNewReports()).isEqualTo(3L);
            assertThat(response.unansweredQuestions()).isEqualTo(7L);
            assertThat(response.totalFaqs()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("getMembers 메서드")
    class GetMembersTest {

        @Test
        @DisplayName("전체 회원 목록을 조회한다")
        void getMembers_전체_성공() {
            // given
            Member member = createTestMember(1L, MemberStatus.ACTIVE);
            Page<Member> memberPage = new PageImpl<>(List.of(member));

            given(memberRepository.findByDeletedAtIsNullOrderByCreatedAtDesc(any(Pageable.class)))
                    .willReturn(memberPage);
            given(feedPostRepository.countByMemberId(anyLong())).willReturn(5L);
            given(commentRepository.countByMemberId(anyLong())).willReturn(10L);
            given(memberFollowRepository.countByFollowingId(anyLong())).willReturn(20L);
            given(memberFollowRepository.countByFollowerId(anyLong())).willReturn(15L);
            given(reportRepository.countReceivedReportsByMemberId(anyLong())).willReturn(0L);
            given(reportRepository.countByReporterId(anyLong())).willReturn(2L);

            // when
            AdminMemberListResponse response = adminService.getMembers(null, null, 0, 10);

            // then
            assertThat(response.members()).hasSize(1);
            assertThat(response.totalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("상태별 회원 목록을 필터링하여 조회한다")
        void getMembers_상태필터_성공() {
            // given
            Member suspendedMember = createTestMember(1L, MemberStatus.SUSPENDED);
            Page<Member> memberPage = new PageImpl<>(List.of(suspendedMember));

            given(memberRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                    eq(MemberStatus.SUSPENDED), any(Pageable.class)))
                    .willReturn(memberPage);
            given(feedPostRepository.countByMemberId(anyLong())).willReturn(0L);
            given(commentRepository.countByMemberId(anyLong())).willReturn(0L);
            given(memberFollowRepository.countByFollowingId(anyLong())).willReturn(0L);
            given(memberFollowRepository.countByFollowerId(anyLong())).willReturn(0L);
            given(reportRepository.countReceivedReportsByMemberId(anyLong())).willReturn(0L);
            given(reportRepository.countByReporterId(anyLong())).willReturn(0L);

            // when
            AdminMemberListResponse response = adminService.getMembers(MemberStatus.SUSPENDED, null, 0, 10);

            // then
            assertThat(response.members()).hasSize(1);
        }

        @Test
        @DisplayName("검색어로 회원을 검색한다")
        void getMembers_검색_성공() {
            // given
            Member member = createTestMember(1L, MemberStatus.ACTIVE);
            Page<Member> memberPage = new PageImpl<>(List.of(member));

            given(memberRepository.searchForAdmin(eq("테스트"), any(Pageable.class)))
                    .willReturn(memberPage);
            given(feedPostRepository.countByMemberId(anyLong())).willReturn(0L);
            given(commentRepository.countByMemberId(anyLong())).willReturn(0L);
            given(memberFollowRepository.countByFollowingId(anyLong())).willReturn(0L);
            given(memberFollowRepository.countByFollowerId(anyLong())).willReturn(0L);
            given(reportRepository.countReceivedReportsByMemberId(anyLong())).willReturn(0L);
            given(reportRepository.countByReporterId(anyLong())).willReturn(0L);

            // when
            AdminMemberListResponse response = adminService.getMembers(null, "테스트", 0, 10);

            // then
            assertThat(response.members()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("sanctionMember 메서드")
    class SanctionMemberTest {

        @Test
        @DisplayName("회원을 일시 정지시킨다")
        void sanctionMember_정지_성공() {
            // given
            Member member = createTestMember(1L, MemberStatus.ACTIVE);
            Member admin = createAdminMember(99L);
            MemberSanctionRequest request = new MemberSanctionRequest(1L, "SUSPEND", 7, "규칙 위반");

            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when
            adminService.sanctionMember(request, admin);

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
            then(memberRepository).should(times(1)).save(member);
        }

        @Test
        @DisplayName("회원을 영구 정지시킨다")
        void sanctionMember_영구정지_성공() {
            // given
            Member member = createTestMember(1L, MemberStatus.ACTIVE);
            Member admin = createAdminMember(99L);
            MemberSanctionRequest request = new MemberSanctionRequest(1L, "SUSPEND", null, "심각한 위반");

            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when
            adminService.sanctionMember(request, admin);

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
        }

        @Test
        @DisplayName("회원을 밴 처리한다")
        void sanctionMember_밴_성공() {
            // given
            Member member = createTestMember(1L, MemberStatus.ACTIVE);
            Member admin = createAdminMember(99L);
            MemberSanctionRequest request = new MemberSanctionRequest(1L, "BAN", null, "영구 차단");

            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when
            adminService.sanctionMember(request, admin);

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.BANNED);
        }

        @Test
        @DisplayName("존재하지 않는 회원 제재 시 예외가 발생한다")
        void sanctionMember_회원없음_예외() {
            // given
            Member admin = createAdminMember(99L);
            MemberSanctionRequest request = new MemberSanctionRequest(999L, "SUSPEND", 7, "위반");

            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminService.sanctionMember(request, admin))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("unbanMember 메서드")
    class UnbanMemberTest {

        @Test
        @DisplayName("회원 정지를 해제한다")
        void unbanMember_성공() {
            // given
            Member member = createTestMember(1L, MemberStatus.SUSPENDED);
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when
            adminService.unbanMember(1L);

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            then(memberRepository).should(times(1)).save(member);
        }
    }

    @Nested
    @DisplayName("getReports 메서드")
    class GetReportsTest {

        @Test
        @DisplayName("전체 신고 목록을 조회한다")
        void getReports_전체_성공() {
            // given
            Member reporter = createTestMember(1L, MemberStatus.ACTIVE);
            Report report = createTestReport(1L, reporter, ReportTargetType.POST, 10L);
            Page<Report> reportPage = new PageImpl<>(List.of(report));

            given(reportRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                    .willReturn(reportPage);

            // when
            AdminReportListResponse response = adminService.getReports(null, 0, 10);

            // then
            assertThat(response.reports()).hasSize(1);
            assertThat(response.totalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("상태별 신고 목록을 필터링하여 조회한다")
        void getReports_상태필터_성공() {
            // given
            Member reporter = createTestMember(1L, MemberStatus.ACTIVE);
            Report report = createTestReport(1L, reporter, ReportTargetType.POST, 10L);
            Page<Report> reportPage = new PageImpl<>(List.of(report));

            given(reportRepository.findByStatusOrderByCreatedAtDesc(eq(ReportStatus.PENDING), any(Pageable.class)))
                    .willReturn(reportPage);

            // when
            AdminReportListResponse response = adminService.getReports(ReportStatus.PENDING, 0, 10);

            // then
            assertThat(response.reports()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("processReport 메서드")
    class ProcessReportTest {

        @Test
        @DisplayName("신고를 기각 처리한다")
        void processReport_기각_성공() {
            // given
            Member reporter = createTestMember(1L, MemberStatus.ACTIVE);
            Member admin = createAdminMember(99L);
            Report report = createTestReport(1L, reporter, ReportTargetType.POST, 10L);

            ReportActionRequest request = new ReportActionRequest(1L, "DISMISS", "허위 신고", null);

            given(reportRepository.findById(1L)).willReturn(Optional.of(report));

            // when
            adminService.processReport(request, admin);

            // then
            assertThat(report.getStatus()).isEqualTo(ReportStatus.DISMISSED);
            then(reportRepository).should(times(1)).save(report);
        }

        @Test
        @DisplayName("신고 처리 시 콘텐츠를 삭제한다")
        void processReport_콘텐츠삭제_성공() {
            // given
            Member reporter = createTestMember(1L, MemberStatus.ACTIVE);
            Member admin = createAdminMember(99L);
            Report report = createTestReport(1L, reporter, ReportTargetType.POST, 10L);

            ReportActionRequest request = new ReportActionRequest(1L, "DELETE_CONTENT", "부적절한 콘텐츠", null);

            given(reportRepository.findById(1L)).willReturn(Optional.of(report));

            // when
            adminService.processReport(request, admin);

            // then
            assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
            then(feedPostRepository).should(times(1)).deleteById(10L);
        }

        @Test
        @DisplayName("존재하지 않는 신고 처리 시 예외가 발생한다")
        void processReport_신고없음_예외() {
            // given
            Member admin = createAdminMember(99L);
            ReportActionRequest request = new ReportActionRequest(999L, "DISMISS", null, null);

            given(reportRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminService.processReport(request, admin))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.REPORT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("answerQuestion 메서드")
    class AnswerQuestionTest {

        @Test
        @DisplayName("문의에 답변을 등록한다")
        void answerQuestion_성공() {
            // given
            Member member = createTestMember(1L, MemberStatus.ACTIVE);
            Question question = createTestQuestion(1L, member);
            QuestionAnswerRequest request = new QuestionAnswerRequest(1L, "답변 내용입니다.");

            given(questionRepository.findById(1L)).willReturn(Optional.of(question));

            // when
            adminService.answerQuestion(request);

            // then
            assertThat(question.getStatus()).isEqualTo(QuestionStatus.ANSWERED);
            assertThat(question.getAnswer()).isEqualTo("답변 내용입니다.");
            then(questionRepository).should(times(1)).save(question);
        }

        @Test
        @DisplayName("존재하지 않는 문의 답변 시 예외가 발생한다")
        void answerQuestion_문의없음_예외() {
            // given
            QuestionAnswerRequest request = new QuestionAnswerRequest(999L, "답변");

            given(questionRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminService.answerQuestion(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.QUESTION_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("saveFaq 메서드")
    class SaveFaqTest {

        @Test
        @DisplayName("새 FAQ를 생성한다")
        void saveFaq_생성_성공() {
            // given
            FAQSaveRequest request = new FAQSaveRequest(
                    null, "SERVICE", "새 질문", "새 답변", 1, true
            );

            FAQ savedFaq = FAQ.builder()
                    .id(1L)
                    .question("새 질문")
                    .answer("새 답변")
                    .category(FAQCategory.SERVICE)
                    .displayOrder(1)
                    .isPublished(true)
                    .build();

            given(faqRepository.save(any(FAQ.class))).willReturn(savedFaq);

            // when
            AdminFAQResponse response = adminService.saveFaq(request);

            // then
            assertThat(response.question()).isEqualTo("새 질문");
            then(faqRepository).should(times(1)).save(any(FAQ.class));
        }

        @Test
        @DisplayName("기존 FAQ를 수정한다")
        void saveFaq_수정_성공() {
            // given
            FAQ existingFaq = createTestFaq(1L, FAQCategory.SERVICE);
            FAQSaveRequest request = new FAQSaveRequest(
                    1L, "ACCOUNT", "수정된 질문", "수정된 답변", 2, false
            );

            given(faqRepository.findById(1L)).willReturn(Optional.of(existingFaq));
            given(faqRepository.save(any(FAQ.class))).willReturn(existingFaq);

            // when
            AdminFAQResponse response = adminService.saveFaq(request);

            // then
            assertThat(existingFaq.getQuestion()).isEqualTo("수정된 질문");
            assertThat(existingFaq.getAnswer()).isEqualTo("수정된 답변");
            assertThat(existingFaq.getCategory()).isEqualTo(FAQCategory.ACCOUNT);
            assertThat(existingFaq.getIsPublished()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 FAQ 수정 시 예외가 발생한다")
        void saveFaq_FAQ없음_예외() {
            // given
            FAQSaveRequest request = new FAQSaveRequest(
                    999L, "SERVICE", "질문", "답변", 1, true
            );

            given(faqRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminService.saveFaq(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.FAQ_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("deleteFaq 메서드")
    class DeleteFaqTest {

        @Test
        @DisplayName("FAQ를 삭제한다")
        void deleteFaq_성공() {
            // given
            given(faqRepository.existsById(1L)).willReturn(true);

            // when
            adminService.deleteFaq(1L);

            // then
            then(faqRepository).should(times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("존재하지 않는 FAQ 삭제 시 예외가 발생한다")
        void deleteFaq_FAQ없음_예외() {
            // given
            given(faqRepository.existsById(999L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> adminService.deleteFaq(999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.FAQ_NOT_FOUND));

            then(faqRepository).should(never()).deleteById(anyLong());
        }
    }
}
