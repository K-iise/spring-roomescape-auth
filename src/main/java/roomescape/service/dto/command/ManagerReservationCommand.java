package roomescape.service.dto.command;

import java.time.LocalDate;
import roomescape.controller.dto.request.ReservationRequest;

public record ManagerReservationCommand(
        LocalDate date,
        Long timeId,
        Long themeId
) {
    public static ManagerReservationCommand from(ReservationRequest request) {
        return new ManagerReservationCommand(request.date(), request.timeId(), request.themeId());
    }
}
