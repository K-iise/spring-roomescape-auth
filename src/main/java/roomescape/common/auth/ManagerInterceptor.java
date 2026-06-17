package roomescape.common.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import roomescape.common.exception.ForbiddenException;
import roomescape.common.exception.UnauthorizedException;
import roomescape.service.StoreService;

@Component
public class ManagerInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final StoreService storeService;

    public ManagerInterceptor(JwtTokenProvider jwtTokenProvider, StoreService storeService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.storeService = storeService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = AuthorizationExtractor.extract(request);
        if (token == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        Long memberId = jwtTokenProvider.getMemberId(token);
        if (storeService.findManagedStore(memberId).isEmpty()) {
            throw new ForbiddenException("매장 관리 권한이 없습니다.");
        }
        return true;
    }
}
