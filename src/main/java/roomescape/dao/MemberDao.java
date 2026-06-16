package roomescape.dao;

import static roomescape.dao.rowMapper.MemberMapper.MEMBER_ROW_MAPPER;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import roomescape.domain.member.Member;

@Repository
public class MemberDao {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    public MemberDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("member")
                .usingGeneratedKeyColumns("id");
    }

    public Optional<Member> findByEmail(String email) {
        String sql = """
                SELECT id, email, password, name
                FROM member
                WHERE email = ?
                """;

        return jdbcTemplate.query(sql, MEMBER_ROW_MAPPER, email)
                .stream()
                .findFirst();
    }

    public Optional<Member> findById(Long id) {
        String sql = """
                SELECT id, email, password, name
                FROM member
                WHERE id = ?
                """;

        return jdbcTemplate.query(sql, MEMBER_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    public Member save(Member member) {
        Map<String, Object> params = new HashMap<>();
        params.put("email", member.getEmail().value());
        params.put("password", member.getPassword().value());
        params.put("name", member.getName().value());

        Long id = jdbcInsert.executeAndReturnKey(params).longValue();

        return new Member(
                id,
                member.getEmail(),
                member.getPassword(),
                member.getName()
        );
    }
}