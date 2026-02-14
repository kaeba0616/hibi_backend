package com.hibi.server.domain.auth.dto.response;

import com.hibi.server.domain.member.entity.UserRoleType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record SignInResponse(
        @NotNull String accessToken,
        @NotNull String refreshToken,
        @NotNull Long memberId,
        @NotNull UserRoleType roleType
) {

    public static SignInResponse of(final String accessToken, final String refreshToken, final Long memberId, final UserRoleType roleType) {
        return SignInResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .memberId(memberId)
                .roleType(roleType)
                .build();
    }
}