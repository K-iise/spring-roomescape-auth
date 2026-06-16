package roomescape.domain.member;

public record MemberName(
        String value
) {
    public static final int NAME_MAX_LENGTH = 100;

    public MemberName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("이름은 비어 있을 수 없습니다.");
        }

        if (value.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("이름은 %d자를 초과할 수 없습니다.".formatted(NAME_MAX_LENGTH));
        }
    }

    public static MemberName parse(String value) {
        return new MemberName(value);
    }
}