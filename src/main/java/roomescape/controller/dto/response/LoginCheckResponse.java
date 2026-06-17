package roomescape.controller.dto.response;

import java.util.Optional;
import roomescape.common.auth.LoginMember;
import roomescape.service.dto.result.StoreResult;

public record LoginCheckResponse(
        String name,
        StoreInfo store
) {
    public static LoginCheckResponse of(LoginMember member, Optional<StoreResult> store) {
        return new LoginCheckResponse(
                member.name(),
                store.map(StoreInfo::from).orElse(null)
        );
    }

    public record StoreInfo(
            Long id,
            String name
    ) {
        public static StoreInfo from(StoreResult result) {
            return new StoreInfo(result.id(), result.name());
        }
    }
}
