package roomescape.service.dto.command;

import java.time.LocalDate;
import roomescape.common.auth.LoginMember;
import roomescape.controller.dto.request.WaitingRequest;

public record WaitingCommand(
        Long memberId,
        String memberName,
        LocalDate date,
        Long timeId,
        Long themeId
) {
    public static WaitingCommand of(WaitingRequest request, LoginMember member) {
        return new WaitingCommand(member.id(), member.name(), request.date(), request.timeId(), request.themeId());
    }
}
