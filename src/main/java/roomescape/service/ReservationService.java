package roomescape.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.exception.ConflictException;
import roomescape.common.exception.ForbiddenException;
import roomescape.common.exception.NotFoundException;
import roomescape.common.exception.UnprocessableEntityException;
import roomescape.dao.ReservationDao;
import roomescape.dao.ReservationTimeDao;
import roomescape.dao.StoreDao;
import roomescape.dao.ThemeDao;
import roomescape.dao.WaitingDao;
import roomescape.dao.dto.WaitingQueryResult;
import roomescape.domain.reservation.Reservation;
import roomescape.domain.reservation.UserName;
import roomescape.domain.reservation.theme.Theme;
import roomescape.domain.reservation.time.ReservationTime;
import roomescape.domain.store.Store;
import roomescape.service.dto.command.ManagerReservationCommand;
import roomescape.service.dto.command.ReservationCommand;
import roomescape.service.dto.result.ReservationDetailResult;
import roomescape.service.dto.result.ReservationDetailResults;
import roomescape.service.dto.result.ReservationResult;

@Service
@Transactional(readOnly = true)
public class ReservationService {
    private final ReservationDao reservationDao;
    private final ReservationTimeDao reservationTimeDao;
    private final ThemeDao themeDao;
    private final WaitingDao waitingDao;
    private final StoreDao storeDao;
    private final Clock clock;

    public ReservationService(
            ReservationDao reservationDao,
            ReservationTimeDao reservationTimeDao,
            ThemeDao themeDao,
            WaitingDao waitingDao,
            StoreDao storeDao,
            Clock clock
    ) {
        this.reservationDao = reservationDao;
        this.reservationTimeDao = reservationTimeDao;
        this.themeDao = themeDao;
        this.waitingDao = waitingDao;
        this.storeDao = storeDao;
        this.clock = clock;
    }

    public List<ReservationResult> findReservations() {
        List<Reservation> reservations = reservationDao.findAll();

        return reservations.stream()
                .map(ReservationResult::from)
                .toList();
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public ReservationDetailResults findReservationDetailsByMemberId(Long memberId) {
        List<Reservation> reservations = reservationDao.findAllByMemberId(memberId);
        List<WaitingQueryResult> waitings = waitingDao.findAllByMemberId(memberId);

        List<ReservationDetailResult> details = Stream.concat(
                reservations.stream().map(ReservationDetailResult::fromReservation),
                waitings.stream().map(ReservationDetailResult::fromWaiting)
        ).toList();

        return new ReservationDetailResults(details);
    }

    @Transactional
    public ReservationResult reserve(ReservationCommand command) {
        Reservation reservation = convertToReservation(null, command);
        validateNoWaiting(command.date(), command.timeId(), command.themeId());
        try {
            Reservation reserved = reservationDao.save(reservation);
            return ReservationResult.from(reserved);
        } catch (DuplicateKeyException e) {
            throw new ConflictException("이미 예약된 시간입니다. 다시 시도해주세요.");
        }
    }

    @Transactional
    public ReservationResult changeReservationSlot(Long id, ReservationCommand command) {
        Reservation origin = getReservationOrThrow(id);
        origin.validateOwner(command.memberId());
        validatePastTime(origin.getDate(), origin.getTime());
        Reservation modified = convertToReservation(id, command);
        validateNoWaiting(command.date(), command.timeId(), command.themeId());
        boolean updated;
        try {
            updated = reservationDao.update(modified);
        } catch (DuplicateKeyException e) {
            throw new ConflictException("이미 예약된 시간입니다. 다시 시도해주세요.");
        }
        if (!updated) {
            throw new NotFoundException("변경하고자 하는 예약이 존재하지 않습니다.");
        }

        promoteFirstWaiting(origin.getDate(), origin.getTime(), origin.getTheme());
        return ReservationResult.from(modified);
    }

    @Transactional
    public void removeReservation(Long id) {
        Reservation origin = getReservationOrThrow(id);
        if (!reservationDao.delete(id)) {
            return;
        }
        if (isPast(origin.getDate(), origin.getTime())) {
            return;
        }
        promoteFirstWaiting(origin.getDate(), origin.getTime(), origin.getTheme());
    }

    @Transactional
    public void cancelReservation(Long id, Long memberId) {
        Reservation origin = getReservationOrThrow(id);
        origin.validateOwner(memberId);
        validatePastTime(origin.getDate(), origin.getTime());
        if (!reservationDao.delete(id)) {
            return;
        }
        promoteFirstWaiting(origin.getDate(), origin.getTime(), origin.getTheme());
    }

    private Reservation convertToReservation(Long id, ReservationCommand command) {
        ReservationTime time = getReservationTimeOrThrow(command.timeId());
        Theme theme = getThemeOrThrow(command.themeId());
        validateAvailability(command.date(), time, theme);

        return new Reservation(
                id,
                command.memberId(),
                UserName.parse(command.memberName()),
                command.date(),
                time,
                theme
        );
    }

    private void promoteFirstWaiting(LocalDate date, ReservationTime time, Theme theme) {
        waitingDao.findFirstBySlot(date, time.getId(), theme.getId()).ifPresent(
                waiting -> {
                    reservationDao.save(new Reservation(waiting.getMemberId(), waiting.getName(), date, time, theme));
                    waitingDao.delete(waiting.getId());
                }
        );
    }

    private void validateAvailability(LocalDate date, ReservationTime time, Theme theme) {
        validatePastTime(date, time);
        validateDuplicate(date, time, theme);
    }

    private boolean isPast(LocalDate date, ReservationTime time) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime requestDateTime = LocalDateTime.of(date, time.getStartAt());
        return requestDateTime.isBefore(now);
    }

    private void validatePastTime(LocalDate date, ReservationTime time) {
        if (isPast(date, time)) {
            throw new UnprocessableEntityException("이미 지난 시간입니다.");
        }
    }

    private void validateDuplicate(LocalDate date, ReservationTime time, Theme theme) {
        if (reservationDao.existsBy(date, theme, time)) {
            throw new ConflictException("이미 존재하는 예약 건입니다.");
        }
    }

    private void validateNoWaiting(LocalDate date, Long timeId, Long themeId) {
        if (waitingDao.existsBySlot(date, timeId, themeId)) {
            throw new ConflictException("이미 예약 대기자가 있는 시간입니다, 예약 대기로 신청해주세요.");
        }
    }

    private Reservation getReservationOrThrow(Long id) {
        return reservationDao.findById(id)
                .orElseThrow(() -> new NotFoundException("삭제하려는 예약이 존재하지 않습니다."));
    }

    private Theme getThemeOrThrow(Long themeId) {
        return themeDao.findThemeById(themeId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 테마입니다."));
    }

    private ReservationTime getReservationTimeOrThrow(Long timeId) {
        return reservationTimeDao.findTimeById(timeId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 시간입니다."));
    }

    public List<ReservationResult> findReservationsByManager(Long memberId) {
        Store store = getManagedStoreOrThrow(memberId);

        return reservationDao.findAllByStoreId(store.getId()).stream()
                .map(ReservationResult::from)
                .toList();
    }

    @Transactional
    public ReservationResult changeReservationByManager(Long memberId, Long id, ManagerReservationCommand command) {
        Store store = getManagedStoreOrThrow(memberId);
        Reservation origin = getReservationWithStoreOrThrow(id);
        origin.validateManagedBy(store);
        validatePastTime(origin.getDate(), origin.getTime());

        ReservationTime time = getReservationTimeOrThrow(command.timeId());
        Theme theme = getThemeOrThrow(command.themeId());
        validateTargetStore(store, command.themeId());
        validateAvailability(command.date(), time, theme);
        validateNoWaiting(command.date(), command.timeId(), command.themeId());

        Reservation modified = new Reservation(id, origin.getMemberId(), origin.getName(), command.date(), time, theme);
        boolean updated;
        try {
            updated = reservationDao.update(modified);
        } catch (DuplicateKeyException e) {
            throw new ConflictException("이미 예약된 시간입니다. 다시 시도해주세요.");
        }
        if (!updated) {
            throw new NotFoundException("변경하고자 하는 예약이 존재하지 않습니다.");
        }

        promoteFirstWaiting(origin.getDate(), origin.getTime(), origin.getTheme());
        return ReservationResult.from(modified);
    }

    @Transactional
    public void removeReservationByManager(Long memberId, Long id) {
        Store store = getManagedStoreOrThrow(memberId);
        Reservation origin = getReservationWithStoreOrThrow(id);
        origin.validateManagedBy(store);
        if (!reservationDao.delete(id)) {
            return;
        }
        if (isPast(origin.getDate(), origin.getTime())) {
            return;
        }
        promoteFirstWaiting(origin.getDate(), origin.getTime(), origin.getTheme());
    }

    private Store getManagedStoreOrThrow(Long memberId) {
        return storeDao.findByManagerMemberId(memberId)
                .orElseThrow(() -> new ForbiddenException("매장 관리 권한이 없습니다."));
    }

    private Reservation getReservationWithStoreOrThrow(Long id) {
        return reservationDao.findByIdWithStore(id)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 예약입니다."));
    }

    private void validateTargetStore(Store store, Long themeId) {
        Long targetStoreId = themeDao.findStoreIdByThemeId(themeId).orElse(null);
        if (!store.canManage(targetStoreId)) {
            throw new ForbiddenException("다른 매장의 테마로는 변경할 수 없습니다.");
        }
    }
}