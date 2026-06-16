package roomescape.domain.member;

public record Password(
        String value
) {
    public Password {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 비어 있을 수 없습니다.");
        }
    }

    public static Password parse(String value) {
        return new Password(value);
    }

    public boolean matches(String rawPassword) {
        return value.equals(rawPassword);
    }
}