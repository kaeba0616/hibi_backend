package com.hibi.server.domain.admin.controller;

import com.hibi.server.domain.admin.dto.request.*;
import com.hibi.server.domain.admin.dto.response.*;
import com.hibi.server.domain.admin.service.AdminService;
import com.hibi.server.domain.faq.entity.FAQCategory;
import com.hibi.server.domain.member.entity.Member;
import com.hibi.server.domain.member.entity.MemberStatus;
import com.hibi.server.domain.question.entity.QuestionStatus;
import com.hibi.server.domain.report.entity.ReportStatus;
import com.hibi.server.global.response.SuccessResponse;
import com.hibi.server.domain.auth.dto.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 API 컨트롤러 (F12)
 */
@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ========== 대시보드 ==========

    @Operation(summary = "대시보드 통계 조회")
    @GetMapping("/stats")
    public ResponseEntity<SuccessResponse<AdminStatsResponse>> getStats() {
        AdminStatsResponse stats = adminService.getStats();
        return ResponseEntity.ok(SuccessResponse.success("통계 조회 성공", stats));
    }

    // ========== 회원 관리 ==========

    @Operation(summary = "회원 목록 조회")
    @GetMapping("/members")
    public ResponseEntity<SuccessResponse<AdminMemberListResponse>> getMembers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        MemberStatus memberStatus = status != null ? MemberStatus.valueOf(status) : null;
        AdminMemberListResponse response = adminService.getMembers(memberStatus, search, page, size);
        return ResponseEntity.ok(SuccessResponse.success("회원 목록 조회 성공", response));
    }

    @Operation(summary = "회원 상세 조회")
    @GetMapping("/members/{memberId}")
    public ResponseEntity<SuccessResponse<AdminMemberResponse>> getMemberDetail(
            @PathVariable Long memberId
    ) {
        AdminMemberResponse response = adminService.getMemberDetail(memberId);
        return ResponseEntity.ok(SuccessResponse.success("회원 상세 조회 성공", response));
    }

    @Operation(summary = "회원 제재 (정지/강제탈퇴)")
    @PostMapping("/members/sanction")
    public ResponseEntity<SuccessResponse<?>> sanctionMember(
            @RequestBody @Valid MemberSanctionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        adminService.sanctionMember(request, userDetails.getMember());
        return ResponseEntity.ok(SuccessResponse.success("회원 제재 완료"));
    }

    @Operation(summary = "회원 정지 해제")
    @PostMapping("/members/{memberId}/unban")
    public ResponseEntity<SuccessResponse<?>> unbanMember(
            @PathVariable Long memberId
    ) {
        adminService.unbanMember(memberId);
        return ResponseEntity.ok(SuccessResponse.success("정지 해제 완료"));
    }

    // ========== 신고 관리 ==========

    @Operation(summary = "신고 목록 조회")
    @GetMapping("/reports")
    public ResponseEntity<SuccessResponse<AdminReportListResponse>> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ReportStatus reportStatus = status != null ? ReportStatus.valueOf(status) : null;
        AdminReportListResponse response = adminService.getReports(reportStatus, page, size);
        return ResponseEntity.ok(SuccessResponse.success("신고 목록 조회 성공", response));
    }

    @Operation(summary = "신고 상세 조회")
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<SuccessResponse<AdminReportResponse>> getReportDetail(
            @PathVariable Long reportId
    ) {
        AdminReportResponse response = adminService.getReportDetail(reportId);
        return ResponseEntity.ok(SuccessResponse.success("신고 상세 조회 성공", response));
    }

    @Operation(summary = "신고 처리")
    @PostMapping("/reports/process")
    public ResponseEntity<SuccessResponse<?>> processReport(
            @RequestBody @Valid ReportActionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        adminService.processReport(request, userDetails.getMember());
        return ResponseEntity.ok(SuccessResponse.success("신고 처리 완료"));
    }

    // ========== 문의 관리 ==========

    @Operation(summary = "문의 목록 조회")
    @GetMapping("/questions")
    public ResponseEntity<SuccessResponse<AdminQuestionListResponse>> getQuestions(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        QuestionStatus questionStatus = status != null ? QuestionStatus.valueOf(status) : null;
        AdminQuestionListResponse response = adminService.getQuestions(questionStatus, page, size);
        return ResponseEntity.ok(SuccessResponse.success("문의 목록 조회 성공", response));
    }

    @Operation(summary = "문의 상세 조회")
    @GetMapping("/questions/{questionId}")
    public ResponseEntity<SuccessResponse<AdminQuestionResponse>> getQuestionDetail(
            @PathVariable Long questionId
    ) {
        AdminQuestionResponse response = adminService.getQuestionDetail(questionId);
        return ResponseEntity.ok(SuccessResponse.success("문의 상세 조회 성공", response));
    }

    @Operation(summary = "문의 답변")
    @PostMapping("/questions/answer")
    public ResponseEntity<SuccessResponse<?>> answerQuestion(
            @RequestBody @Valid QuestionAnswerRequest request
    ) {
        adminService.answerQuestion(request);
        return ResponseEntity.ok(SuccessResponse.success("답변 등록 완료"));
    }

    // ========== FAQ 관리 ==========

    @Operation(summary = "FAQ 목록 조회 (관리자용)")
    @GetMapping("/faqs")
    public ResponseEntity<SuccessResponse<AdminFAQListResponse>> getFaqs(
            @RequestParam(required = false) String category
    ) {
        FAQCategory faqCategory = category != null ? FAQCategory.valueOf(category) : null;
        AdminFAQListResponse response = adminService.getFaqs(faqCategory);
        return ResponseEntity.ok(SuccessResponse.success("FAQ 목록 조회 성공", response));
    }

    @Operation(summary = "FAQ 생성/수정")
    @PostMapping("/faqs")
    public ResponseEntity<SuccessResponse<AdminFAQResponse>> saveFaq(
            @RequestBody @Valid FAQSaveRequest request
    ) {
        AdminFAQResponse response = adminService.saveFaq(request);
        String message = request.isCreate() ? "FAQ 생성 완료" : "FAQ 수정 완료";
        return ResponseEntity.ok(SuccessResponse.success(message, response));
    }

    @Operation(summary = "FAQ 삭제")
    @DeleteMapping("/faqs/{faqId}")
    public ResponseEntity<SuccessResponse<?>> deleteFaq(
            @PathVariable Long faqId
    ) {
        adminService.deleteFaq(faqId);
        return ResponseEntity.ok(SuccessResponse.success("FAQ 삭제 완료"));
    }
}
