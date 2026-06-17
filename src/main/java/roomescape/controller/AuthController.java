package roomescape.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import roomescape.common.auth.JwtTokenProvider;
import roomescape.common.auth.Login;
import roomescape.common.auth.LoginMember;
import roomescape.controller.dto.request.LoginRequest;
import roomescape.controller.dto.response.LoginCheckResponse;
import roomescape.controller.dto.response.TokenResponse;
import roomescape.service.AuthService;
import roomescape.service.dto.command.LoginCommand;
import roomescape.service.dto.result.MemberResult;

@RestController
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        MemberResult member = authService.login(LoginCommand.from(request));

        String accessToken = jwtTokenProvider.createToken(member.id());

        return ResponseEntity.ok(new TokenResponse(accessToken));
    }

    @GetMapping("/login/check")
    public ResponseEntity<LoginCheckResponse> checkLogin(@Login LoginMember member) {
        return ResponseEntity.ok(LoginCheckResponse.from(member));
    }
}
