package roomescape.service.dto.command;

import java.time.LocalDate;
import roomescape.controller.dto.request.ReservationRequest;

public record ReservationCommand(
        String name,
        LocalDate date,
        Long timeId,
        Long themeId
) {
    public static ReservationCommand of(ReservationRequest request, String name) {
        return new ReservationCommand(name, request.date(), request.timeId(), request.themeId());
    }
}