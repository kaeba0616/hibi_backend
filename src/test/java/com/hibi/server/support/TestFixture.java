package com.hibi.server.support;

import com.hibi.server.domain.member.entity.Member;
import com.hibi.server.domain.member.entity.ProviderType;
import com.hibi.server.domain.member.entity.UserRoleType;
import com.hibi.server.domain.member.entity.MemberStatus;

/**
 * 테스트용 데이터 생성 헬퍼 클래스
 */
public class TestFixture {

    /**
     * 기본 테스트 회원 생성
     */
    public static Member createMember() {
        return Member.builder()
                .email("test@example.com")
                .password("encodedPassword123")
                .nickname("테스트유저")
                .provider(ProviderType.NATIVE)
                .role(UserRoleType.USER)
                .status(MemberStatus.ACTIVE)
                .build();
    }

    /**
     * 커스텀 이메일로 회원 생성
     */
    public static Member createMember(String email) {
        return Member.builder()
                .email(email)
                .password("encodedPassword123")
                .nickname("테스트유저_" + email.split("@")[0])
                .provider(ProviderType.NATIVE)
                .role(UserRoleType.USER)
                .status(MemberStatus.ACTIVE)
                .build();
    }

    /**
     * 관리자 회원 생성
     */
    public static Member createAdminMember() {
        return Member.builder()
                .email("admin@example.com")
                .password("encodedPassword123")
                .nickname("관리자")
                .provider(ProviderType.NATIVE)
                .role(UserRoleType.ADMIN)
                .status(MemberStatus.ACTIVE)
                .build();
    }

    /**
     * 정지된 회원 생성
     */
    public static Member createSuspendedMember() {
        return Member.builder()
                .email("suspended@example.com")
                .password("encodedPassword123")
                .nickname("정지회원")
                .provider(ProviderType.NATIVE)
                .role(UserRoleType.USER)
                .status(MemberStatus.SUSPENDED)
                .build();
    }
}
