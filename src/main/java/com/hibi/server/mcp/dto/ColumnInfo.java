package com.hibi.server.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnInfo {
    private String columnName;
    private String dataType;
    private boolean nullable;
    private String defaultValue;
    private boolean isPrimaryKey;
}
