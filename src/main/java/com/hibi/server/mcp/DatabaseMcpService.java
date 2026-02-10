package com.hibi.server.mcp;

import com.hibi.server.mcp.dto.ColumnInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 데이터베이스 검사 도구 서비스
 * 테이블 상태 조회 및 데이터 검사 기능 제공
 */
@Slf4j
@Service
@Profile("mcp")
@RequiredArgsConstructor
public class DatabaseMcpService {

    private final JdbcTemplate jdbcTemplate;

    @Tool(description = "데이터베이스의 모든 테이블 목록 조회")
    public List<String> getAllTables() {
        try {
            String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE()";
            return jdbcTemplate.queryForList(sql, String.class);
        } catch (Exception e) {
            log.error("테이블 목록 조회 실패", e);
            return List.of("Error: " + e.getMessage());
        }
    }

    @Tool(description = "특정 테이블의 행 수 조회")
    public Map<String, Object> getTableRowCount(
            @ToolParam(description = "테이블명 (예: members, posts)") String tableName) {

        Map<String, Object> result = new HashMap<>();
        result.put("tableName", tableName);

        try {
            // SQL Injection 방지를 위한 테이블명 검증
            if (!isValidTableName(tableName)) {
                result.put("error", "유효하지 않은 테이블명입니다.");
                return result;
            }

            String sql = "SELECT COUNT(*) FROM " + tableName;
            Long count = jdbcTemplate.queryForObject(sql, Long.class);

            result.put("rowCount", count);
            result.put("success", true);

        } catch (Exception e) {
            log.error("테이블 행 수 조회 실패: {}", tableName, e);
            result.put("error", e.getMessage());
            result.put("success", false);
        }

        return result;
    }

    @Tool(description = "특정 테이블의 스키마(컬럼) 정보 조회")
    public List<ColumnInfo> getTableSchema(
            @ToolParam(description = "테이블명 (예: members, posts)") String tableName) {

        List<ColumnInfo> columns = new ArrayList<>();

        try {
            if (!isValidTableName(tableName)) {
                log.warn("유효하지 않은 테이블명: {}", tableName);
                return columns;
            }

            String sql = """
                SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_KEY
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND TABLE_SCHEMA = DATABASE()
                ORDER BY ORDINAL_POSITION
                """;

            return jdbcTemplate.query(sql, (rs, rowNum) -> ColumnInfo.builder()
                            .columnName(rs.getString("COLUMN_NAME"))
                            .dataType(rs.getString("DATA_TYPE"))
                            .nullable("YES".equals(rs.getString("IS_NULLABLE")))
                            .defaultValue(rs.getString("COLUMN_DEFAULT"))
                            .isPrimaryKey("PRI".equals(rs.getString("COLUMN_KEY")))
                            .build(),
                    tableName);

        } catch (Exception e) {
            log.error("테이블 스키마 조회 실패: {}", tableName, e);
            return columns;
        }
    }

    @Tool(description = "특정 테이블의 최근 N개 레코드 조회")
    public List<Map<String, Object>> getRecentRecords(
            @ToolParam(description = "테이블명 (예: members, posts)") String tableName,
            @ToolParam(description = "조회할 레코드 수 (최대 100)") int limit) {

        try {
            if (!isValidTableName(tableName)) {
                return List.of(Map.of("error", "유효하지 않은 테이블명입니다."));
            }

            // 보안을 위해 limit 제한
            int safeLimit = Math.min(Math.max(1, limit), 100);

            String sql = "SELECT * FROM " + tableName + " ORDER BY id DESC LIMIT " + safeLimit;
            return jdbcTemplate.queryForList(sql);

        } catch (Exception e) {
            log.error("최근 레코드 조회 실패: {}", tableName, e);
            return List.of(Map.of("error", e.getMessage()));
        }
    }

    @Tool(description = "데이터베이스 전체 통계 조회")
    public Map<String, Object> getDatabaseStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            List<String> tables = getAllTables();
            stats.put("totalTables", tables.size());

            Map<String, Long> tableRowCounts = new HashMap<>();
            long totalRecords = 0;

            for (String table : tables) {
                if (!table.startsWith("Error")) {
                    try {
                        Long count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM " + table, Long.class);
                        if (count != null) {
                            tableRowCounts.put(table, count);
                            totalRecords += count;
                        }
                    } catch (Exception e) {
                        tableRowCounts.put(table, -1L);
                    }
                }
            }

            stats.put("totalRecords", totalRecords);
            stats.put("tableRowCounts", tableRowCounts);
            stats.put("success", true);

        } catch (Exception e) {
            log.error("데이터베이스 통계 조회 실패", e);
            stats.put("error", e.getMessage());
            stats.put("success", false);
        }

        return stats;
    }

    @Tool(description = "안전한 SELECT 쿼리 실행 (SELECT만 허용)")
    public List<Map<String, Object>> executeSelectQuery(
            @ToolParam(description = "SELECT 쿼리 (SELECT로 시작해야 함)") String query) {

        try {
            String trimmedQuery = query.trim().toUpperCase();

            // SELECT 쿼리만 허용
            if (!trimmedQuery.startsWith("SELECT")) {
                return List.of(Map.of("error", "SELECT 쿼리만 허용됩니다."));
            }

            // 위험한 키워드 차단
            if (containsDangerousKeywords(trimmedQuery)) {
                return List.of(Map.of("error", "허용되지 않는 SQL 키워드가 포함되어 있습니다."));
            }

            // 결과 제한
            if (!trimmedQuery.contains("LIMIT")) {
                query = query.trim();
                if (query.endsWith(";")) {
                    query = query.substring(0, query.length() - 1);
                }
                query += " LIMIT 100";
            }

            return jdbcTemplate.queryForList(query);

        } catch (Exception e) {
            log.error("쿼리 실행 실패: {}", query, e);
            return List.of(Map.of("error", e.getMessage()));
        }
    }

    private boolean isValidTableName(String tableName) {
        // 테이블명은 알파벳, 숫자, 언더스코어만 허용
        return tableName != null && tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    private boolean containsDangerousKeywords(String query) {
        String[] dangerous = {"INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE",
                "ALTER", "CREATE", "GRANT", "REVOKE", "EXECUTE", "--", "/*"};
        for (String keyword : dangerous) {
            if (query.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
