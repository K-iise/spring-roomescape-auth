package roomescape.domain.member;

import java.util.Objects;

public class Member {
    private final Long id;
    private final Email email;
    private final Password password;
    private final MemberName name;

    public Member(Long id, Email email, Password password, MemberName name) {
        validate(email, password, name);
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    private void validate(Email email, Password password, MemberName name) {
        Objects.requireNonNull(email, "이메일이 비어 있습니다.");
        Objects.requireNonNull(password, "비밀번호가 비어 있습니다.");
        Objects.requireNonNull(name, "이름이 비어 있습니다.");
    }

    public boolean matchesPassword(String rawPassword) {
        return password.matches(rawPassword);
    }

    public Long getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public Password getPassword() {
        return password;
    }

    public MemberName getName() {
        return name;
    }
}