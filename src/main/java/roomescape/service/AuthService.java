package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.exception.NotFoundException;
import roomescape.common.exception.UnauthorizedException;
import roomescape.dao.MemberDao;
import roomescape.domain.member.Member;
import roomescape.service.dto.command.LoginCommand;
import roomescape.service.dto.result.MemberResult;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final MemberDao memberDao;

    public AuthService(MemberDao memberDao) {
        this.memberDao = memberDao;
    }

    public MemberResult login(LoginCommand command) {
        Member member = memberDao.findByEmail(command.email())
                .orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!member.matchesPassword(command.password())) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return MemberResult.from(member);
    }

    public MemberResult findById(Long id) {
        Member member = memberDao.findById(id)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 회원입니다."));

        return MemberResult.from(member);
    }
}