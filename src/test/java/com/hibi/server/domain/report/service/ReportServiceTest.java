package com.hibi.server.domain.report.service;

import com.hibi.server.domain.comment.entity.Comment;
import com.hibi.server.domain.comment.repository.CommentRepository;
import com.hibi.server.domain.feedpost.entity.FeedPost;
import com.hibi.server.domain.feedpost.repository.FeedPostRepository;
import com.hibi.server.domain.member.entity.Member;
import com.hibi.server.domain.member.entity.MemberStatus;
import com.hibi.server.domain.member.entity.ProviderType;
import com.hibi.server.domain.member.entity.UserRoleType;
import com.hibi.server.domain.member.repository.MemberRepository;
import com.hibi.server.domain.report.dto.request.ReportCreateRequest;
import com.hibi.server.domain.report.dto.response.ReportCheckResponse;
import com.hibi.server.domain.report.dto.response.ReportResponse;
import com.hibi.server.domain.report.entity.Report;
import com.hibi.server.domain.report.entity.ReportReason;
import com.hibi.server.domain.report.entity.ReportTargetType;
import com.hibi.server.domain.report.repository.ReportRepository;
import com.hibi.server.support.ServiceTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@DisplayName("ReportService 단위 테스트")
class ReportServiceTest extends ServiceTestSupport {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private FeedPostRepository feedPostRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private ReportService reportService;

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

    @Nested
    @DisplayName("createReport 메서드")
    class CreateReportTest {

        @Test
        @DisplayName("게시글 신고가 성공한다")
        void createReport_게시글_성공() {
            // given
            Long reporterId = 1L;
            Long authorId = 2L;
            Long postId = 10L;

            Member reporter = createTestMember(reporterId);
            Member author = createTestMember(authorId);
            FeedPost post = createTestPost(postId, author);

            ReportCreateRequest request = new ReportCreateRequest(
                    "POST", postId, "SPAM", "스팸 게시글입니다"
            );

            Report savedReport = Report.builder()
                    .id(1L)
                    .reporter(reporter)
                    .targetType(ReportTargetType.POST)
                    .targetId(postId)
                    .reason(ReportReason.SPAM)
                    .description("스팸 게시글입니다")
                    .build();

            given(memberRepository.findById(reporterId)).willReturn(Optional.of(reporter));
            given(feedPostRepository.findById(postId)).willReturn(Optional.of(post));
            given(feedPostRepository.existsById(postId)).willReturn(true);
            given(reportRepository.save(any(Report.class))).willReturn(savedReport);

            // when
            ReportResponse response = reportService.createReport(request, reporterId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.targetType()).isEqualTo("POST");
            then(reportRepository).should(times(1)).save(any(Report.class));
        }

        @Test
        @DisplayName("댓글 신고가 성공한다")
        void createReport_댓글_성공() {
            // given
            Long reporterId = 1L;
            Long authorId = 2L;
            Long commentId = 10L;

            Member reporter = createTestMember(reporterId);
            Member author = createTestMember(authorId);
            FeedPost post = createTestPost(1L, author);
            Comment comment = createTestComment(commentId, post, author);

            ReportCreateRequest request = new ReportCreateRequest(
                    "COMMENT", commentId, "ABUSE", "욕설 댓글"
            );

            Report savedReport = Report.builder()
                    .id(1L)
                    .reporter(reporter)
                    .targetType(ReportTargetType.COMMENT)
                    .targetId(commentId)
                    .reason(ReportReason.ABUSE)
                    .description("욕설 댓글")
                    .build();

            given(memberRepository.findById(reporterId)).willReturn(Optional.of(reporter));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.existsById(commentId)).willReturn(true);
            given(reportRepository.save(any(Report.class))).willReturn(savedReport);

            // when
            ReportResponse response = reportService.createReport(request, reporterId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.targetType()).isEqualTo("COMMENT");
        }

        @Test
        @DisplayName("본인 게시글 신고 시 예외가 발생한다")
        void createReport_본인게시글_예외() {
            // given
            Long memberId = 1L;
            Long postId = 10L;

            Member member = createTestMember(memberId);
            FeedPost post = createTestPost(postId, member);

            ReportCreateRequest request = new ReportCreateRequest(
                    "POST", postId, "SPAM", "테스트"
            );

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(feedPostRepository.findById(postId)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> reportService.createReport(request, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인의 게시글은 신고할 수 없습니다");

            then(reportRepository).should(never()).save(any(Report.class));
        }

        @Test
        @DisplayName("본인 신고 시 예외가 발생한다")
        void createReport_본인_예외() {
            // given
            Long memberId = 1L;
            Member member = createTestMember(memberId);

            ReportCreateRequest request = new ReportCreateRequest(
                    "MEMBER", memberId, "ABUSE", "본인 신고 시도"
            );

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> reportService.createReport(request, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인을 신고할 수 없습니다");
        }

        @Test
        @DisplayName("존재하지 않는 대상 신고 시 예외가 발생한다")
        void createReport_대상없음_예외() {
            // given
            Long reporterId = 1L;
            Long postId = 999L;

            Member reporter = createTestMember(reporterId);

            ReportCreateRequest request = new ReportCreateRequest(
                    "POST", postId, "SPAM", "테스트"
            );

            given(memberRepository.findById(reporterId)).willReturn(Optional.of(reporter));
            given(feedPostRepository.findById(postId)).willReturn(Optional.empty());
            given(feedPostRepository.existsById(postId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> reportService.createReport(request, reporterId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("checkAlreadyReported 메서드")
    class CheckAlreadyReportedTest {

        @Test
        @DisplayName("이미 신고한 경우 true를 반환한다")
        void checkAlreadyReported_이미신고_true() {
            // given
            Long reporterId = 1L;
            Long targetId = 10L;

            given(reportRepository.existsByReporterIdAndTarget(
                    eq(reporterId), eq(ReportTargetType.POST), eq(targetId)))
                    .willReturn(true);

            // when
            ReportCheckResponse response = reportService.checkAlreadyReported(
                    reporterId, "POST", targetId
            );

            // then
            assertThat(response.alreadyReported()).isTrue();
        }

        @Test
        @DisplayName("신고하지 않은 경우 false를 반환한다")
        void checkAlreadyReported_신고안함_false() {
            // given
            Long reporterId = 1L;
            Long targetId = 10L;

            given(reportRepository.existsByReporterIdAndTarget(
                    eq(reporterId), eq(ReportTargetType.POST), eq(targetId)))
                    .willReturn(false);

            // when
            ReportCheckResponse response = reportService.checkAlreadyReported(
                    reporterId, "POST", targetId
            );

            // then
            assertThat(response.alreadyReported()).isFalse();
        }
    }
}
