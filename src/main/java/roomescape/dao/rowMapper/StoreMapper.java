package roomescape.dao.rowMapper;

import org.springframework.jdbc.core.RowMapper;
import roomescape.domain.store.Store;

public final class StoreMapper {

    public static final RowMapper<Store> STORE_ROW_MAPPER = (rs, rowNum) -> new Store(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getLong("manager_member_id")
    );

    private StoreMapper() {
    }
}
