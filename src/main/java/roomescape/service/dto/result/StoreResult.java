package roomescape.service.dto.result;

import roomescape.domain.store.Store;

public record StoreResult(
        Long id,
        String name
) {
    public static StoreResult from(Store store) {
        return new StoreResult(store.getId(), store.getName());
    }
}
