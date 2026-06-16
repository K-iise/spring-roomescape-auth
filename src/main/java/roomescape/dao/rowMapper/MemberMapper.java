package roomescape.dao.rowMapper;

import org.springframework.jdbc.core.RowMapper;
import roomescape.domain.member.Email;
import roomescape.domain.member.Member;
import roomescape.domain.member.MemberName;
import roomescape.domain.member.Password;

public final class MemberMapper {

    public static final RowMapper<Member> MEMBER_ROW_MAPPER = (rs, rowNum) -> {
        return new Member(
                rs.getLong("id"),
                Email.parse(rs.getString("email")),
                Password.parse(rs.getString("password")),
                MemberName.parse(rs.getString("name"))
        );
    };

    private MemberMapper() {
    }
}