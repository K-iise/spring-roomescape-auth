package roomescape.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import roomescape.common.auth.SessionConst;
import roomescape.common.exception.UnauthorizedException;
import roomescape.controller.dto.request.LoginRequest;
import roomescape.controller.dto.response.LoginCheckResponse;
import roomescape.service.AuthService;
import roomescape.service.dto.command.LoginCommand;
import roomescape.service.dto.result.MemberResult;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        MemberResult member = authService.login(LoginCommand.from(request));

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(SessionConst.MEMBER_ID, member.id());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/login/check")
    public ResponseEntity<LoginCheckResponse> checkLogin(HttpServletRequest httpRequest) {
        Long memberId = extractMemberId(httpRequest);

        MemberResult member = authService.findById(memberId);

        return ResponseEntity.ok(LoginCheckResponse.from(member));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.noContent().build();
    }

    private Long extractMemberId(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute(SessionConst.MEMBER_ID) == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        return (Long) session.getAttribute(SessionConst.MEMBER_ID);
    }
}