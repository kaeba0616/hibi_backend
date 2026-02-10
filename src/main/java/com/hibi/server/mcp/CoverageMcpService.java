package com.hibi.server.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 커버리지 리포트 도구 서비스
 * JaCoCo 커버리지 결과 조회 기능 제공
 */
@Slf4j
@Service
@Profile("mcp")
public class CoverageMcpService {

    private static final String PROJECT_DIR = System.getProperty("user.dir");
    private static final String JACOCO_XML_PATH = "build/reports/jacoco/test/jacocoTestReport.xml";
    private static final String JACOCO_HTML_PATH = "build/reports/jacoco/test/html";

    @Tool(description = "전체 JaCoCo 커버리지 리포트 조회")
    public Map<String, Object> getCoverageReport() {
        Map<String, Object> report = new HashMap<>();

        File xmlFile = new File(PROJECT_DIR, JACOCO_XML_PATH);

        if (!xmlFile.exists()) {
            report.put("error", "커버리지 리포트가 없습니다. './gradlew jacocoTestReport' 먼저 실행하세요.");
            report.put("command", "./gradlew jacocoTestReport");
            return report;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);

            // 루트 리포트 요소에서 전체 카운터 추출
            Element root = doc.getDocumentElement();
            NodeList counters = root.getElementsByTagName("counter");

            for (int i = 0; i < counters.getLength(); i++) {
                Element counter = (Element) counters.item(i);
                // 최상위 counter만 (package의 counter가 아닌)
                if (counter.getParentNode().getNodeName().equals("report")) {
                    String type = counter.getAttribute("type");
                    int missed = Integer.parseInt(counter.getAttribute("missed"));
                    int covered = Integer.parseInt(counter.getAttribute("covered"));
                    int total = missed + covered;

                    double percentage = total > 0 ? (covered * 100.0) / total : 0;

                    report.put(type.toLowerCase() + "Coverage", String.format("%.1f%%", percentage));
                    report.put(type.toLowerCase() + "Covered", covered);
                    report.put(type.toLowerCase() + "Missed", missed);
                    report.put(type.toLowerCase() + "Total", total);
                }
            }

            report.put("success", true);
            report.put("reportPath", new File(PROJECT_DIR, JACOCO_HTML_PATH + "/index.html").getAbsolutePath());

        } catch (Exception e) {
            log.error("커버리지 리포트 파싱 실패", e);
            report.put("error", e.getMessage());
            report.put("success", false);
        }

        return report;
    }

    @Tool(description = "특정 패키지의 커버리지 조회")
    public Map<String, Object> getPackageCoverage(
            @ToolParam(description = "패키지 경로 (예: com.hibi.server.domain.auth)") String packagePath) {

        Map<String, Object> coverage = new HashMap<>();
        coverage.put("package", packagePath);

        File xmlFile = new File(PROJECT_DIR, JACOCO_XML_PATH);

        if (!xmlFile.exists()) {
            coverage.put("error", "커버리지 리포트가 없습니다.");
            return coverage;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);

            NodeList packages = doc.getElementsByTagName("package");
            boolean found = false;

            for (int i = 0; i < packages.getLength(); i++) {
                Element pkg = (Element) packages.item(i);
                String pkgName = pkg.getAttribute("name").replace("/", ".");

                if (pkgName.equals(packagePath) || pkgName.startsWith(packagePath)) {
                    found = true;
                    NodeList counters = pkg.getElementsByTagName("counter");

                    for (int j = 0; j < counters.getLength(); j++) {
                        Element counter = (Element) counters.item(j);
                        // 직접 자식 counter만
                        if (counter.getParentNode().equals(pkg)) {
                            String type = counter.getAttribute("type");
                            int missed = Integer.parseInt(counter.getAttribute("missed"));
                            int covered = Integer.parseInt(counter.getAttribute("covered"));
                            int total = missed + covered;

                            double percentage = total > 0 ? (covered * 100.0) / total : 0;
                            coverage.put(type.toLowerCase() + "Coverage", String.format("%.1f%%", percentage));
                        }
                    }
                    break;
                }
            }

            if (!found) {
                coverage.put("warning", "해당 패키지를 찾을 수 없습니다.");
            }

            coverage.put("success", true);

        } catch (Exception e) {
            log.error("패키지 커버리지 조회 실패: {}", packagePath, e);
            coverage.put("error", e.getMessage());
            coverage.put("success", false);
        }

        return coverage;
    }

    @Tool(description = "클래스별 커버리지 목록 조회")
    public List<Map<String, Object>> getClassCoverageList(
            @ToolParam(description = "패키지 경로 필터 (예: com.hibi.server.domain). 비어있으면 전체") String packageFilter) {

        List<Map<String, Object>> classList = new ArrayList<>();

        File xmlFile = new File(PROJECT_DIR, JACOCO_XML_PATH);

        if (!xmlFile.exists()) {
            classList.add(Map.of("error", "커버리지 리포트가 없습니다."));
            return classList;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);

            NodeList classes = doc.getElementsByTagName("class");

            for (int i = 0; i < classes.getLength(); i++) {
                Element clazz = (Element) classes.item(i);
                String className = clazz.getAttribute("name").replace("/", ".");

                // 필터 적용
                if (packageFilter != null && !packageFilter.isEmpty()
                        && !className.startsWith(packageFilter)) {
                    continue;
                }

                Map<String, Object> classInfo = new HashMap<>();
                classInfo.put("className", className);

                NodeList counters = clazz.getElementsByTagName("counter");
                for (int j = 0; j < counters.getLength(); j++) {
                    Element counter = (Element) counters.item(j);
                    if (counter.getParentNode().equals(clazz)) {
                        String type = counter.getAttribute("type");
                        if (type.equals("LINE") || type.equals("BRANCH")) {
                            int missed = Integer.parseInt(counter.getAttribute("missed"));
                            int covered = Integer.parseInt(counter.getAttribute("covered"));
                            int total = missed + covered;
                            double percentage = total > 0 ? (covered * 100.0) / total : 0;
                            classInfo.put(type.toLowerCase() + "Coverage", String.format("%.1f%%", percentage));
                        }
                    }
                }

                classList.add(classInfo);
            }

        } catch (Exception e) {
            log.error("클래스 커버리지 목록 조회 실패", e);
            classList.add(Map.of("error", e.getMessage()));
        }

        return classList;
    }

    @Tool(description = "커버리지가 낮은 클래스 목록 조회 (개선 필요)")
    public List<Map<String, Object>> getLowCoverageClasses(
            @ToolParam(description = "기준 커버리지 (0-100, 기본 50)") int threshold) {

        List<Map<String, Object>> lowCoverageClasses = new ArrayList<>();

        int safeThreshold = Math.min(Math.max(0, threshold), 100);
        if (threshold == 0) safeThreshold = 50;

        File xmlFile = new File(PROJECT_DIR, JACOCO_XML_PATH);

        if (!xmlFile.exists()) {
            lowCoverageClasses.add(Map.of("error", "커버리지 리포트가 없습니다."));
            return lowCoverageClasses;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);

            NodeList classes = doc.getElementsByTagName("class");

            for (int i = 0; i < classes.getLength(); i++) {
                Element clazz = (Element) classes.item(i);
                String className = clazz.getAttribute("name").replace("/", ".");

                NodeList counters = clazz.getElementsByTagName("counter");
                for (int j = 0; j < counters.getLength(); j++) {
                    Element counter = (Element) counters.item(j);
                    if (counter.getParentNode().equals(clazz) && counter.getAttribute("type").equals("LINE")) {
                        int missed = Integer.parseInt(counter.getAttribute("missed"));
                        int covered = Integer.parseInt(counter.getAttribute("covered"));
                        int total = missed + covered;

                        if (total > 0) {
                            double percentage = (covered * 100.0) / total;
                            if (percentage < safeThreshold) {
                                Map<String, Object> classInfo = new HashMap<>();
                                classInfo.put("className", className);
                                classInfo.put("lineCoverage", String.format("%.1f%%", percentage));
                                classInfo.put("linesCovered", covered);
                                classInfo.put("linesMissed", missed);
                                lowCoverageClasses.add(classInfo);
                            }
                        }
                        break;
                    }
                }
            }

            // 커버리지 낮은 순으로 정렬
            lowCoverageClasses.sort((a, b) -> {
                String coverageA = (String) a.getOrDefault("lineCoverage", "100%");
                String coverageB = (String) b.getOrDefault("lineCoverage", "100%");
                double valueA = Double.parseDouble(coverageA.replace("%", ""));
                double valueB = Double.parseDouble(coverageB.replace("%", ""));
                return Double.compare(valueA, valueB);
            });

        } catch (Exception e) {
            log.error("낮은 커버리지 클래스 조회 실패", e);
            lowCoverageClasses.add(Map.of("error", e.getMessage()));
        }

        return lowCoverageClasses;
    }
}
