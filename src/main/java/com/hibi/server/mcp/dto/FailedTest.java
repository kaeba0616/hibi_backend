package com.hibi.server.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedTest {
    private String testClass;
    private String testMethod;
    private String errorMessage;
    private String stackTrace;
}
