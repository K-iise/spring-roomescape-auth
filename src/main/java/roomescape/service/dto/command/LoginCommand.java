package roomescape.service.dto.command;

import roomescape.controller.dto.request.LoginRequest;

public record LoginCommand(
        String email,
        String password
) {
    public static LoginCommand from(LoginRequest request) {
        return new LoginCommand(request.email(), request.password());
    }
}