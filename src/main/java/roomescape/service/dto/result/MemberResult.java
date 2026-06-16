package roomescape.service.dto.result;

import roomescape.domain.member.Member;

public record MemberResult(
        Long id,
        String email,
        String name
) {
    public static MemberResult from(Member member) {
        return new MemberResult(
                member.getId(),
                member.getEmail().value(),
                member.getName().value()
        );
    }
}