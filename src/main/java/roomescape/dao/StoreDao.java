package roomescape.dao;

import static roomescape.dao.rowMapper.StoreMapper.STORE_ROW_MAPPER;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import roomescape.domain.store.Store;

@Repository
public class StoreDao {

    private final JdbcTemplate jdbcTemplate;

    public StoreDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Store> findByManagerMemberId(Long managerMemberId) {
        return jdbcTemplate.query(
                        "SELECT id, name, manager_member_id FROM store WHERE manager_member_id = ?",
                        STORE_ROW_MAPPER,
                        managerMemberId
                ).stream()
                .findFirst();
    }
}
