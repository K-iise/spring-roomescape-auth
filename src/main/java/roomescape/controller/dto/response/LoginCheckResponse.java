package roomescape.controller.dto.response;

import roomescape.common.auth.LoginMember;

public record LoginCheckResponse(
        String name
) {
    public static LoginCheckResponse from(LoginMember member) {
        return new LoginCheckResponse(member.name());
    }
}
