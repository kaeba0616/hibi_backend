package com.hibi.server.domain.report.repository;

import com.hibi.server.domain.member.entity.Member;
import com.hibi.server.domain.report.entity.Report;
import com.hibi.server.domain.report.entity.ReportStatus;
import com.hibi.server.domain.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 신고 Repository (F11)
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * 특정 사용자가 특정 대상에 대해 이미 신고했는지 확인 (AC-F11-7)
     */
    boolean existsByReporterAndTargetTypeAndTargetId(
            Member reporter,
            ReportTargetType targetType,
            Long targetId
    );

    /**
     * 특정 사용자가 특정 대상에 대해 이미 신고했는지 확인 (reporter ID로)
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
           "FROM Report r " +
           "WHERE r.reporter.id = :reporterId " +
           "AND r.targetType = :targetType " +
           "AND r.targetId = :targetId")
    boolean existsByReporterIdAndTarget(
            @Param("reporterId") Long reporterId,
            @Param("targetType") ReportTargetType targetType,
            @Param("targetId") Long targetId
    );

    /**
     * 특정 대상에 대한 신고 목록 조회
     */
    List<Report> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            ReportTargetType targetType,
            Long targetId
    );

    /**
     * 특정 대상에 대한 신고 수 조회
     */
    long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);

    /**
     * 상태별 신고 목록 조회 (페이징)
     */
    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    /**
     * 대기중인 신고 목록 조회 (관리자용)
     */
    @Query("SELECT r FROM Report r " +
           "WHERE r.status = :status " +
           "ORDER BY r.createdAt ASC")
    Page<Report> findPendingReports(@Param("status") ReportStatus status, Pageable pageable);

    /**
     * 특정 사용자의 신고 목록 조회
     */
    List<Report> findByReporterOrderByCreatedAtDesc(Member reporter);

    /**
     * 특정 게시글에 대한 신고 조회
     */
    default List<Report> findByPostId(Long postId) {
        return findByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTargetType.POST, postId);
    }

    /**
     * 특정 댓글에 대한 신고 조회
     */
    default List<Report> findByCommentId(Long commentId) {
        return findByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTargetType.COMMENT, commentId);
    }

    /**
     * 특정 사용자에 대한 신고 조회
     */
    default List<Report> findByMemberId(Long memberId) {
        return findByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTargetType.MEMBER, memberId);
    }

    // ========== F12 관리자 기능용 쿼리 메서드 ==========

    /**
     * 전체 신고 목록 조회 (관리자용)
     */
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 대기중 신고 수 조회 (대시보드용)
     */
    long countByStatus(ReportStatus status);

    /**
     * 오늘 접수된 신고 수 조회 (대시보드용)
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.createdAt >= CURRENT_DATE")
    long countTodayReports();

    /**
     * 특정 회원이 받은 신고 수 조회
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.targetType = 'MEMBER' AND r.targetId = :memberId")
    long countReceivedReportsByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 회원이 보낸 신고 수 조회
     */
    long countByReporterId(Long reporterId);
}
