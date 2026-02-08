package com.hibi.server.support;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Repository 테스트를 위한 베이스 클래스
 *
 * 이 클래스를 상속받으면:
 * - JPA 관련 컴포넌트만 로드 (가벼운 컨텍스트)
 * - H2 In-Memory DB 사용
 * - 각 테스트 후 트랜잭션 롤백
 *
 * 사용 예시:
 * class MemberRepositoryTest extends RepositoryTestSupport {
 *     @Autowired
 *     private MemberRepository memberRepository;
 * }
 */
@DataJpaTest
@ActiveProfiles("test")
public abstract class RepositoryTestSupport {
    // JPA 관련 컴포넌트만 로드
    // 필요 시 공통 유틸리티 메서드 추가
}
