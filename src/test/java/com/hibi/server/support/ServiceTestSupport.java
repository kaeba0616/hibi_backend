package com.hibi.server.support;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 서비스 단위 테스트를 위한 베이스 클래스
 *
 * 이 클래스를 상속받으면:
 * - Mockito Extension 활성화
 * - @Mock, @InjectMocks 사용 가능
 *
 * 사용 예시:
 * class MyServiceTest extends ServiceTestSupport {
 *     @Mock
 *     private MyRepository myRepository;
 *
 *     @InjectMocks
 *     private MyService myService;
 * }
 */
@ExtendWith(MockitoExtension.class)
public abstract class ServiceTestSupport {
    // Mockito Extension만 활성화
    // 필요 시 공통 유틸리티 메서드 추가
}
