package com.hibi.server.mcp;

import com.hibi.server.mcp.dto.FailedTest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP 테스트 실행 도구 서비스
 * Gradle 테스트 실행 및 결과 조회 기능 제공
 */
@Slf4j
@Service
@Profile("mcp")
public class TestMcpService {

    private static final String PROJECT_DIR = System.getProperty("user.dir");
    private static final String TEST_REPORT_PATH = "build/reports/tests/test";

    @Tool(description = "Gradle 테스트 실행. 필터가 비어있으면 전체 테스트 실행")
    public Map<String, Object> runTests(
            @ToolParam(description = "테스트 클래스 필터 (예: AuthServiceTest). 비어있으면 전체 실행") String filter) {

        Map<String, Object> result = new HashMap<>();

        try {
            List<String> command = new ArrayList<>();
            command.add("./gradlew");
            command.add("test");

            if (filter != null && !filter.trim().isEmpty()) {
                command.add("--tests");
                command.add("*" + filter + "*");
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(PROJECT_DIR));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            result.put("success", exitCode == 0);
            result.put("exitCode", exitCode);
            result.put("output", output.toString());
            result.put("filter", filter != null ? filter : "전체");

            // 테스트 결과 요약 추가
            if (exitCode == 0) {
                result.putAll(getTestSummary());
            }

        } catch (Exception e) {
            log.error("테스트 실행 실패", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    @Tool(description = "최근 테스트 실행 결과 요약 조회")
    public Map<String, Object> getTestSummary() {
        Map<String, Object> summary = new HashMap<>();

        File reportDir = new File(PROJECT_DIR, TEST_REPORT_PATH);
        File indexHtml = new File(reportDir, "index.html");

        if (!indexHtml.exists()) {
            summary.put("error", "테스트 리포트가 없습니다. './gradlew test' 먼저 실행하세요.");
            return summary;
        }

        try {
            String content = readFileContent(indexHtml);

            // HTML에서 테스트 결과 파싱
            Pattern testsPattern = Pattern.compile("(\\d+)\\s+tests");
            Pattern failuresPattern = Pattern.compile("(\\d+)\\s+failures");
            Pattern ignoredPattern = Pattern.compile("(\\d+)\\s+ignored");
            Pattern durationPattern = Pattern.compile("(\\d+\\.?\\d*)s");

            Matcher testsMatcher = testsPattern.matcher(content);
            Matcher failuresMatcher = failuresPattern.matcher(content);
            Matcher ignoredMatcher = ignoredPattern.matcher(content);
            Matcher durationMatcher = durationPattern.matcher(content);

            int totalTests = testsMatcher.find() ? Integer.parseInt(testsMatcher.group(1)) : 0;
            int failures = failuresMatcher.find() ? Integer.parseInt(failuresMatcher.group(1)) : 0;
            int ignored = ignoredMatcher.find() ? Integer.parseInt(ignoredMatcher.group(1)) : 0;
            int passed = totalTests - failures - ignored;

            summary.put("totalTests", totalTests);
            summary.put("passed", passed);
            summary.put("failed", failures);
            summary.put("skipped", ignored);
            summary.put("successRate", totalTests > 0 ? String.format("%.1f%%", (passed * 100.0) / totalTests) : "N/A");

            if (durationMatcher.find()) {
                summary.put("duration", durationMatcher.group(0));
            }

            summary.put("reportPath", indexHtml.getAbsolutePath());

        } catch (Exception e) {
            log.error("테스트 결과 파싱 실패", e);
            summary.put("error", e.getMessage());
        }

        return summary;
    }

    @Tool(description = "실패한 테스트 상세 정보 조회")
    public List<FailedTest> getFailedTests() {
        List<FailedTest> failedTests = new ArrayList<>();

        File classesDir = new File(PROJECT_DIR, TEST_REPORT_PATH + "/classes");

        if (!classesDir.exists()) {
            log.warn("테스트 클래스 리포트 디렉토리가 없습니다: {}", classesDir.getAbsolutePath());
            return failedTests;
        }

        File[] htmlFiles = classesDir.listFiles((dir, name) -> name.endsWith(".html"));

        if (htmlFiles == null) {
            return failedTests;
        }

        for (File htmlFile : htmlFiles) {
            try {
                String content = readFileContent(htmlFile);

                // 실패한 테스트만 파싱
                if (content.contains("class=\"failures\"") || content.contains("failures")) {
                    Pattern failedMethodPattern = Pattern.compile(
                            "<td class=\"failures\">.*?<a.*?>([^<]+)</a>.*?</td>",
                            Pattern.DOTALL);
                    Pattern errorPattern = Pattern.compile(
                            "<pre>([^<]+)</pre>",
                            Pattern.DOTALL);

                    Matcher methodMatcher = failedMethodPattern.matcher(content);
                    Matcher errorMatcher = errorPattern.matcher(content);

                    String className = htmlFile.getName().replace(".html", "");

                    while (methodMatcher.find()) {
                        String methodName = methodMatcher.group(1);
                        String errorMessage = errorMatcher.find() ? errorMatcher.group(1).trim() : "Unknown error";

                        failedTests.add(FailedTest.builder()
                                .testClass(className)
                                .testMethod(methodName)
                                .errorMessage(errorMessage)
                                .build());
                    }
                }
            } catch (Exception e) {
                log.error("Failed test 파싱 실패: {}", htmlFile.getName(), e);
            }
        }

        return failedTests;
    }

    private String readFileContent(File file) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
