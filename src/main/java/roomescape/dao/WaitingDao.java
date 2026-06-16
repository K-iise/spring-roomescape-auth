package roomescape.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import roomescape.dao.dto.WaitingQueryResult;
import roomescape.domain.reservation.UserName;
import roomescape.domain.reservation.Waiting;
import roomescape.domain.reservation.theme.Description;
import roomescape.domain.reservation.theme.Theme;
import roomescape.domain.reservation.theme.ThemeName;
import roomescape.domain.reservation.theme.ThumbnailUrl;
import roomescape.domain.reservation.time.ReservationTime;

@Repository
public class WaitingDao {

    private static final RowMapper<Waiting> WAITING_ROW_MAPPER = (rs, rowNum) -> {
        ReservationTime time = new ReservationTime(
                rs.getLong("time_id"),
                rs.getTime("start_at").toLocalTime()
        );
        Theme theme = new Theme(
                rs.getLong("theme_id"),
                ThemeName.parse(rs.getString("theme_name")),
                Description.parse(rs.getString("description")),
                ThumbnailUrl.parse(rs.getString("url"))
        );
        return new Waiting(
                rs.getLong("id"),
                rs.getLong("member_id"),
                UserName.parse(rs.getString("name")),
                rs.getDate("date").toLocalDate(),
                time,
                theme,
                rs.getObject("created_at", LocalDateTime.class)
        );
    };

    private static final RowMapper<WaitingQueryResult> WAITING_SEQUENCE_ROW_MAPPER = (rs, rowNum) -> {
        ReservationTime time = new ReservationTime(
                rs.getLong("time_id"),
                rs.getTime("start_at").toLocalTime()
        );
        Theme theme = new Theme(
                rs.getLong("theme_id"),
                ThemeName.parse(rs.getString("theme_name")),
                Description.parse(rs.getString("description")),
                ThumbnailUrl.parse(rs.getString("url"))
        );
        return new WaitingQueryResult(
                rs.getLong("id"),
                UserName.parse(rs.getString("name")),
                rs.getDate("date").toLocalDate(),
                time,
                theme,
                rs.getObject("created_at", LocalDateTime.class),
                rs.getInt("sequence")
        );
    };

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    public WaitingDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("waiting")
                .usingGeneratedKeyColumns("id");
    }

    public Waiting save(Waiting waiting) {
        Map<String, Object> params = new HashMap<>();
        params.put("member_id", waiting.getMemberId());
        params.put("date", waiting.getDate());
        params.put("time_id", waiting.getTime().getId());
        params.put("theme_id", waiting.getTheme().getId());
        params.put("created_at", waiting.getCreatedAt());

        Long id = jdbcInsert.executeAndReturnKey(params).longValue();
        return new Waiting(
                id,
                waiting.getMemberId(),
                waiting.getName(),
                waiting.getDate(),
                waiting.getTime(),
                waiting.getTheme(),
                waiting.getCreatedAt()
        );
    }

    public void delete(Long id) {
        String sql = """
                DELETE FROM waiting
                       WHERE id = ?
                """;

        jdbcTemplate.update(sql, id);
    }

    public List<WaitingQueryResult> findAllByMemberId(Long memberId) {
        String sql = """
                SELECT id, name, date, created_at,
                       time_id, start_at,
                       theme_id, theme_name, description, url,
                       sequence
                FROM (
                    SELECT w.id, w.member_id, m.name AS name, w.date, w.created_at,
                           rt.id AS time_id, rt.start_at,
                           t.id AS theme_id, t.name AS theme_name, t.description, t.url,
                           ROW_NUMBER() OVER (
                               PARTITION BY w.date, w.time_id, w.theme_id
                               ORDER BY w.created_at, w.id ASC
                           ) AS sequence
                    FROM waiting w
                    INNER JOIN member m ON w.member_id = m.id
                    INNER JOIN reservation_time rt ON w.time_id = rt.id
                    INNER JOIN theme t ON w.theme_id = t.id
                ) ranked
                WHERE member_id = ?
                ORDER BY date, start_at, sequence
                """;

        return jdbcTemplate.query(
                sql,
                WAITING_SEQUENCE_ROW_MAPPER,
                memberId
        );
    }

    public Optional<Waiting> findById(Long id) {
        String sql = """
                SELECT w.id, w.member_id, m.name, w.date, w.created_at,
                       rt.id AS time_id, rt.start_at,
                       t.id AS theme_id, t.name AS theme_name, t.description, t.url
                FROM waiting w
                INNER JOIN member m ON w.member_id = m.id
                INNER JOIN reservation_time rt ON w.time_id = rt.id
                INNER JOIN theme t ON w.theme_id = t.id
                WHERE w.id = ?
                """;

        return jdbcTemplate.query(
                        sql,
                        WAITING_ROW_MAPPER,
                        id
                ).stream()
                .findFirst();
    }

    public boolean existsBySlot(LocalDate date, Long timeId, Long themeId) {
        String sql = """
                SELECT EXISTS(
                            SELECT 1
                            FROM waiting
                            WHERE date = ? AND
                                  time_id = ? AND theme_id = ?
                )
                """;
        Boolean result = jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                date,
                timeId,
                themeId
        );

        return Boolean.TRUE.equals(result);
    }

    public boolean existsBySlotAndMember(Long memberId, LocalDate date, Long timeId, Long themeId) {
        String sql = """
                SELECT EXISTS(
                            SELECT 1
                            FROM waiting
                            WHERE member_id = ? AND date = ? AND
                                  time_id = ? AND theme_id = ?
                )
                """;

        Boolean result = jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                memberId,
                date,
                timeId,
                themeId
        );

        return Boolean.TRUE.equals(result);
    }

    public boolean existsByThemeId(Long themeId) {
        String sql = """
                        SELECT EXISTS(
                            SELECT 1
                            FROM waiting
                            WHERE theme_id = ?
                        )
                """;
        Boolean result = jdbcTemplate.queryForObject(sql, Boolean.class, themeId);
        return Boolean.TRUE.equals(result);
    }

    public boolean existsByTimeId(Long timeId) {
        String sql = """
                                SELECT EXISTS(
                                SELECT 1
                                FROM waiting
                                WHERE time_id = ?
                                )
                """;
        Boolean result = jdbcTemplate.queryForObject(sql, Boolean.class, timeId);
        return Boolean.TRUE.equals(result);
    }

    public Optional<Waiting> findFirstBySlot(LocalDate date, Long timeId, Long themeId) {
        String sql = """
                SELECT w.id, w.member_id, m.name, w.date, w.created_at,
                       rt.id AS time_id, rt.start_at,
                       t.id AS theme_id, t.name AS theme_name, t.description, t.url
                FROM waiting w
                INNER JOIN member m ON w.member_id = m.id
                INNER JOIN reservation_time rt ON w.time_id = rt.id
                INNER JOIN theme t ON w.theme_id = t.id
                WHERE w.date = ? AND w.time_id = ? AND w.theme_id = ?
                ORDER BY w.created_at, w.id ASC
                LIMIT 1
                FOR UPDATE
                """;
        return jdbcTemplate.query(
                        sql,
                        WAITING_ROW_MAPPER,
                        date, timeId, themeId
                ).stream()
                .findFirst();
    }

}
