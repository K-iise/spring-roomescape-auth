package roomescape.domain.store;

import java.util.Objects;

public class Store {

    private final Long id;
    private final String name;
    private final Long managerMemberId;

    public Store(Long id, String name, Long managerMemberId) {
        validate(name, managerMemberId);
        this.id = id;
        this.name = name;
        this.managerMemberId = managerMemberId;
    }

    private void validate(String name, Long managerMemberId) {
        Objects.requireNonNull(name, "매장 이름이 비어 있습니다.");
        Objects.requireNonNull(managerMemberId, "매장 매니저가 비어 있습니다.");
    }

    public boolean canManage(Long themeStoreId) {
        return id.equals(themeStoreId);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getManagerMemberId() {
        return managerMemberId;
    }
}
