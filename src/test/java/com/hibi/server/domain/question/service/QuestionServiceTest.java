package com.hibi.server.domain.question.service;

import com.hibi.server.domain.member.entity.Member;
import com.hibi.server.domain.member.entity.MemberStatus;
import com.hibi.server.domain.member.entity.ProviderType;
import com.hibi.server.domain.member.entity.UserRoleType;
import com.hibi.server.domain.member.repository.MemberRepository;
import com.hibi.server.domain.question.dto.request.QuestionCreateRequest;
import com.hibi.server.domain.question.dto.response.QuestionListResponse;
import com.hibi.server.domain.question.dto.response.QuestionResponse;
import com.hibi.server.domain.question.entity.Question;
import com.hibi.server.domain.question.entity.QuestionStatus;
import com.hibi.server.domain.question.entity.QuestionType;
import com.hibi.server.domain.question.repository.QuestionRepository;
import com.hibi.server.support.ServiceTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@DisplayName("QuestionService 단위 테스트")
class QuestionServiceTest extends ServiceTestSupport {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private QuestionService questionService;

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

    @Nested
    @DisplayName("getMyQuestions 메서드")
    class GetMyQuestionsTest {

        @Test
        @DisplayName("본인의 문의 목록을 조회한다")
        void getMyQuestions_성공() {
            // given
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            Question question1 = createTestQuestion(1L, member);
            Question question2 = createTestQuestion(2L, member);

            given(questionRepository.findByMemberIdOrderByCreatedAtDesc(memberId))
                    .willReturn(List.of(question1, question2));

            // when
            QuestionListResponse response = questionService.getMyQuestions(memberId);

            // then
            assertThat(response.questions()).hasSize(2);
        }

        @Test
        @DisplayName("문의가 없으면 빈 목록을 반환한다")
        void getMyQuestions_빈목록_성공() {
            // given
            Long memberId = 1L;
            given(questionRepository.findByMemberIdOrderByCreatedAtDesc(memberId))
                    .willReturn(List.of());

            // when
            QuestionListResponse response = questionService.getMyQuestions(memberId);

            // then
            assertThat(response.questions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getQuestionById 메서드")
    class GetQuestionByIdTest {

        @Test
        @DisplayName("본인 문의 상세 조회가 성공한다")
        void getQuestionById_본인_성공() {
            // given
            Long memberId = 1L;
            Long questionId = 1L;
            Member member = createTestMember(memberId);
            Question question = createTestQuestion(questionId, member);

            given(questionRepository.findById(questionId)).willReturn(Optional.of(question));

            // when
            QuestionResponse response = questionService.getQuestionById(questionId, memberId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.title()).isEqualTo("테스트 문의");
        }

        @Test
        @DisplayName("타인 문의 조회 시 예외가 발생한다")
        void getQuestionById_타인_예외() {
            // given
            Long ownerId = 1L;
            Long otherMemberId = 2L;
            Long questionId = 1L;
            Member owner = createTestMember(ownerId);
            Question question = createTestQuestion(questionId, owner);

            given(questionRepository.findById(questionId)).willReturn(Optional.of(question));

            // when & then
            assertThatThrownBy(() -> questionService.getQuestionById(questionId, otherMemberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("찾을 수 없습니다");
        }

        @Test
        @DisplayName("존재하지 않는 문의 조회 시 예외가 발생한다")
        void getQuestionById_문의없음_예외() {
            // given
            Long memberId = 1L;
            Long questionId = 999L;

            given(questionRepository.findById(questionId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> questionService.getQuestionById(questionId, memberId))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("createQuestion 메서드")
    class CreateQuestionTest {

        @Test
        @DisplayName("문의 생성이 성공한다")
        void createQuestion_성공() {
            // given
            Long memberId = 1L;
            Member member = createTestMember(memberId);
            QuestionCreateRequest request = new QuestionCreateRequest(
                    "SERVICE", "서비스 문의", "앱 사용 중 문제가 발생했습니다"
            );

            Question savedQuestion = Question.builder()
                    .id(1L)
                    .member(member)
                    .type(QuestionType.SERVICE)
                    .title("서비스 문의")
                    .content("앱 사용 중 문제가 발생했습니다")
                    .status(QuestionStatus.RECEIVED)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(questionRepository.save(any(Question.class))).willReturn(savedQuestion);

            // when
            QuestionResponse response = questionService.createQuestion(request, memberId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.title()).isEqualTo("서비스 문의");
            assertThat(response.status()).isEqualTo("received");
            then(questionRepository).should(times(1)).save(any(Question.class));
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외가 발생한다")
        void createQuestion_회원없음_예외() {
            // given
            Long memberId = 999L;
            QuestionCreateRequest request = new QuestionCreateRequest(
                    "SERVICE", "문의", "내용"
            );

            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> questionService.createQuestion(request, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("회원을 찾을 수 없습니다");
        }
    }
}
