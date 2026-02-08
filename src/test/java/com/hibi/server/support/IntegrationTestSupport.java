package com.hibi.server.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 테스트를 위한 베이스 클래스
 *
 * 이 클래스를 상속받으면:
 * - Spring Boot 전체 컨텍스트 로드
 * - MockMvc 자동 주입
 * - ObjectMapper 자동 주입
 * - 테스트 프로파일(H2 DB) 활성화
 * - 각 테스트 후 트랜잭션 롤백
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
}
