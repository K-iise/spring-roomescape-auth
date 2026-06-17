package roomescape.domain.reservation;

import java.time.LocalDate;
import java.util.Objects;
import roomescape.common.exception.ForbiddenException;
import roomescape.domain.reservation.theme.Theme;
import roomescape.domain.reservation.time.ReservationTime;
import roomescape.domain.store.Store;

public class Reservation {

    private final Long id;
    private final Long memberId;
    private final UserName userName;
    private final LocalDate date;
    private final ReservationTime time;
    private final Theme theme;

    public Reservation(Long memberId, UserName userName, LocalDate date, ReservationTime time, Theme theme) {
        this(null, memberId, userName, date, time, theme);
    }

    public Reservation(Long id, Long memberId, UserName userName, LocalDate date, ReservationTime time, Theme theme) {
        this.id = id;
        validate(memberId, userName, date, time, theme);
        this.memberId = memberId;
        this.userName = userName;
        this.date = date;
        this.time = time;
        this.theme = theme;
    }

    private void validate(Long memberId, UserName userName, LocalDate date, ReservationTime time, Theme theme) {
        Objects.requireNonNull(memberId, "예약자가 비어 있습니다.");
        Objects.requireNonNull(userName, "예약자 이름이 비어 있습니다.");
        Objects.requireNonNull(date, "예약 날짜가 비어 있습니다.");
        Objects.requireNonNull(time, "시간이 비어 있습니다.");
        Objects.requireNonNull(theme, "테마가 비어 있습니다.");
    }

    public void validateOwner(Long memberId) {
        if (!this.memberId.equals(memberId)) {
            throw new ForbiddenException("다른 사람의 예약은 취소/변경할 수 없습니다.");
        }
    }

    public void validateManagedBy(Store store) {
        if (!store.canManage(theme.getStoreId())) {
            throw new ForbiddenException("다른 매장의 예약은 관리할 수 없습니다.");
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
}
