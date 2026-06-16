package roomescape.service.dto.command;

import java.time.LocalDate;
import roomescape.common.auth.LoginMember;
import roomescape.controller.dto.request.ReservationRequest;

public record ReservationCommand(
        Long memberId,
        String memberName,
        LocalDate date,
        Long timeId,
        Long themeId
) {
    public static ReservationCommand of(ReservationRequest request, LoginMember member) {
        return new ReservationCommand(member.id(), member.name(), request.date(), request.timeId(), request.themeId());
    }
}
