package com.hibi.server.domain.report.service;

import com.hibi.server.domain.comment.repository.CommentRepository;
import com.hibi.server.domain.feedpost.repository.FeedPostRepository;
import com.hibi.server.domain.member.entity.Member;
import com.hibi.server.domain.member.repository.MemberRepository;
import com.hibi.server.domain.report.dto.request.ReportCreateRequest;
import com.hibi.server.domain.report.dto.response.ReportCheckResponse;
import com.hibi.server.domain.report.dto.response.ReportResponse;
import com.hibi.server.domain.report.entity.Report;
import com.hibi.server.domain.report.entity.ReportReason;
import com.hibi.server.domain.report.entity.ReportTargetType;
import com.hibi.server.domain.report.repository.ReportRepository;
import com.hibi.server.global.exception.CustomException;
import com.hibi.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신고 Service (F11)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final FeedPostRepository feedPostRepository;
    private final CommentRepository commentRepository;

    /**
     * 신고 생성
     */
    @Transactional
    public ReportResponse createReport(ReportCreateRequest request, Long reporterId) {
        // 신고자 조회
        Member reporter = memberRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 대상 유형 파싱
        ReportTargetType targetType = parseTargetType(request.targetType());

        // 신고 사유 파싱
        ReportReason reason = parseReason(request.reason());

        // 본인 콘텐츠 신고 방지 (AC-F11-8)
        validateNotSelfReport(reporterId, targetType, request.targetId());

        // 대상 존재 여부 확인
        validateTargetExists(targetType, request.targetId());

        // 신고 생성
        Report report = Report.of(
                reporter,
                targetType,
                request.targetId(),
                reason,
                request.description()
        );

        try {
            Report savedReport = reportRepository.save(report);
            return ReportResponse.from(savedReport);
        } catch (DataIntegrityViolationException e) {
            // 중복 신고 (AC-F11-7)
            throw new CustomException(ErrorCode.DUPLICATE_REPORT);
        }
    }

    /**
     * 중복 신고 여부 확인
     */
    public ReportCheckResponse checkAlreadyReported(Long reporterId, String targetType, Long targetId) {
        ReportTargetType type = parseTargetType(targetType);
        boolean alreadyReported = reportRepository.existsByReporterIdAndTarget(reporterId, type, targetId);
        return ReportCheckResponse.of(alreadyReported);
    }

    /**
     * 대상 유형 파싱
     */
    private ReportTargetType parseTargetType(String targetType) {
        try {
            return ReportTargetType.valueOf(targetType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 신고 대상 유형입니다: " + targetType);
        }
    }

    /**
     * 신고 사유 파싱
     */
    private ReportReason parseReason(String reason) {
        try {
            return ReportReason.valueOf(reason.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 신고 사유입니다: " + reason);
        }
    }

    /**
     * 본인 콘텐츠 신고 방지 (AC-F11-8)
     */
    private void validateNotSelfReport(Long reporterId, ReportTargetType targetType, Long targetId) {
        switch (targetType) {
            case POST -> {
                feedPostRepository.findById(targetId).ifPresent(post -> {
                    if (post.getMember().getId().equals(reporterId)) {
                        throw new IllegalArgumentException("본인의 게시글은 신고할 수 없습니다.");
                    }
                });
            }
            case COMMENT -> {
                commentRepository.findById(targetId).ifPresent(comment -> {
                    if (comment.getMember().getId().equals(reporterId)) {
                        throw new IllegalArgumentException("본인의 댓글은 신고할 수 없습니다.");
                    }
                });
            }
            case MEMBER -> {
                if (targetId.equals(reporterId)) {
                    throw new IllegalArgumentException("본인을 신고할 수 없습니다.");
                }
            }
        }
    }

    /**
     * 대상 존재 여부 확인
     */
    private void validateTargetExists(ReportTargetType targetType, Long targetId) {
        boolean exists = switch (targetType) {
            case POST -> feedPostRepository.existsById(targetId);
            case COMMENT -> commentRepository.existsById(targetId);
            case MEMBER -> memberRepository.existsById(targetId);
        };

        if (!exists) {
            String targetName = switch (targetType) {
                case POST -> "게시글";
                case COMMENT -> "댓글";
                case MEMBER -> "사용자";
            };
            throw new IllegalArgumentException(targetName + "을(를) 찾을 수 없습니다.");
        }
    }
}
