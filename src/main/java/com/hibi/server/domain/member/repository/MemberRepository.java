package com.hibi.server.domain.member.repository;

import com.hibi.server.domain.member.entity.Member;
import com.hibi.server.domain.member.entity.MemberStatus;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    Optional<Member> findByEmailAndDeletedAtIsNotNull(String email);

    boolean existsByNickname(String nickname);

    boolean existsById(@NonNull Long id);

    /**
     * 사용자 검색 (닉네임, 이메일 username 부분)
     */
    @Query("SELECT m FROM Member m WHERE m.deletedAt IS NULL AND (" +
           "LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Member> searchByKeyword(@Param("keyword") String keyword);

    // ========== F12 관리자 기능용 쿼리 메서드 ==========

    /**
     * 전체 회원 수 조회 (대시보드용 - 탈퇴 제외)
     */
    long countByDeletedAtIsNull();

    /**
     * 오늘 가입한 회원 수 조회 (대시보드용)
     */
    @Query("SELECT COUNT(m) FROM Member m WHERE m.createdAt >= CURRENT_DATE AND m.deletedAt IS NULL")
    long countTodayNewMembers();

    /**
     * 상태별 회원 목록 조회 (관리자용)
     */
    Page<Member> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(MemberStatus status, Pageable pageable);

    /**
     * 전체 회원 목록 조회 (관리자용)
     */
    Page<Member> findByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 검색어로 회원 목록 조회 (관리자용)
     */
    @Query("SELECT m FROM Member m WHERE m.deletedAt IS NULL AND " +
           "(LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY m.createdAt DESC")
    Page<Member> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 정지 만료된 회원 조회 (자동 해제용)
     */
    @Query("SELECT m FROM Member m WHERE m.status = 'SUSPENDED' AND m.suspendedUntil < CURRENT_TIMESTAMP")
    List<Member> findExpiredSuspensions();
}
