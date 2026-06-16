package roomescape.domain.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import roomescape.common.exception.ForbiddenException;
import roomescape.domain.reservation.theme.Theme;
import roomescape.domain.reservation.time.ReservationTime;

public class Waiting {
    private final Long id;
    private final Long memberId;
    private final UserName userName;
    private final LocalDate date;
    private final ReservationTime time;
    private final Theme theme;
    private final LocalDateTime createdAt;

    public Waiting(Long memberId, UserName userName, LocalDate date, ReservationTime time, Theme theme,
                   LocalDateTime createdAt) {
        this(null, memberId, userName, date, time, theme, createdAt);
    }

    public Waiting(Long id, Long memberId, UserName userName, LocalDate date, ReservationTime time, Theme theme,
                   LocalDateTime createdAt) {
        this.id = id;
        validate(memberId, userName, date, time, theme, createdAt);
        this.memberId = memberId;
        this.userName = userName;
        this.date = date;
        this.time = time;
        this.theme = theme;
        this.createdAt = createdAt;
    }

    private void validate(Long memberId, UserName userName, LocalDate date, ReservationTime time, Theme theme,
                          LocalDateTime createdAt) {
        Objects.requireNonNull(memberId, "예약자가 비어 있습니다.");
        Objects.requireNonNull(userName, "예약자 이름이 비어 있습니다.");
        Objects.requireNonNull(date, "예약 날짜가 비어 있습니다.");
        Objects.requireNonNull(time, "시간이 비어 있습니다.");
        Objects.requireNonNull(theme, "테마가 비어 있습니다.");
        Objects.requireNonNull(createdAt, "대기 신청 시간이 비어 있습니다.");
    }

    public void validateOwner(Long memberId) {
        if (!this.memberId.equals(memberId)) {
            throw new ForbiddenException("다른 사람의 예약 대기는 취소할 수 없습니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public UserName getName() {
        return userName;
    }

    public LocalDate getDate() {
        return date;
    }

    public ReservationTime getTime() {
        return time;
    }

    public Theme getTheme() {
        return theme;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
