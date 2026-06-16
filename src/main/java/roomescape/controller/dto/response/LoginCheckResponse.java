package roomescape.controller.dto.response;

import roomescape.service.dto.result.MemberResult;

public record LoginCheckResponse(
        String name
) {
    public static LoginCheckResponse from(MemberResult result) {
        return new LoginCheckResponse(result.name());
    }
}