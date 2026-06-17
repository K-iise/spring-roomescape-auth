package roomescape.service;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import roomescape.common.exception.ConflictException;
import roomescape.common.exception.ForbiddenException;
import roomescape.common.exception.NotFoundException;
import roomescape.dao.ReservationDao;
import roomescape.dao.StoreDao;
import roomescape.dao.ThemeDao;
import roomescape.dao.WaitingDao;
import roomescape.dao.dto.TimeQueryResult;
import roomescape.domain.reservation.theme.Description;
import roomescape.domain.reservation.theme.Theme;
import roomescape.domain.reservation.theme.ThemeName;
import roomescape.domain.reservation.theme.ThumbnailUrl;
import roomescape.domain.store.Store;
import roomescape.service.dto.command.ThemeCommand;
import roomescape.service.dto.result.ReservationTimeDetailResult;
import roomescape.service.dto.result.ThemeResult;

@Service
@Transactional(readOnly = true)
public class ThemeService {
    private static final String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/images/";

    private final ThemeDao themeDao;
    private final WaitingDao waitingDao;
    private final ReservationDao reservationDao;
    private final StoreDao storeDao;
    private final Clock clock;

    public ThemeService(ThemeDao themeDao, WaitingDao waitingDao, ReservationDao reservationDao, StoreDao storeDao,
                        Clock clock) {
        this.themeDao = themeDao;
        this.waitingDao = waitingDao;
        this.reservationDao = reservationDao;
        this.storeDao = storeDao;
        this.clock = clock;
    }

    public List<ThemeResult> findAllThemes() {
        List<Theme> themes = themeDao.findAllThemes();
        return themes.stream()
                .map(ThemeResult::from)
                .toList();
    }

    public List<ThemeResult> findThemesByManager(Long memberId) {
        Store store = getManagedStoreOrThrow(memberId);
        return themeDao.findAllByStoreId(store.getId()).stream()
                .map(ThemeResult::from)
                .toList();
    }

    public List<ThemeResult> findTopTheme(Long count) {
        LocalDate today = LocalDate.now(clock);
        List<Theme> topTheme = themeDao.findTopThemes(count, today);
        return topTheme.stream()
                .map(ThemeResult::from)
                .toList();
    }

    @Transactional
    public ThemeResult createTheme(Long memberId, ThemeCommand command) {
        Store store = getManagedStoreOrThrow(memberId);

        if (themeDao.existsByName(command.name())) {
            throw new ConflictException("이미 존재하는 테마입니다.");
        }

        MultipartFile file = command.file();
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String filePath = uploadDir + fileName;

        try {
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            file.transferTo(new File(filePath));

        } catch (IOException e) {
            throw new RuntimeException("이미지 저장에 실패했습니다.", e);
        }

        String imageUrl = "/images/" + fileName;

        Theme theme = new Theme(
                null,
                ThemeName.parse(command.name()),
                Description.parse(command.description()),
                ThumbnailUrl.parse(imageUrl),
                store.getId()
        );
        Theme saved = themeDao.save(theme);

        return ThemeResult.from(saved);
    }

    @Transactional
    public void deleteTheme(Long memberId, Long id) {
        Store store = getManagedStoreOrThrow(memberId);
        Theme origin = getThemeOrThrow(id);
        if (!store.canManage(origin.getStoreId())) {
            throw new ForbiddenException("다른 매장의 테마는 관리할 수 없습니다.");
        }

        if (reservationDao.existsByThemeId(id)) {
            throw new ConflictException("예약이 존재하는 테마는 삭제할 수 없습니다.");
        }

        if (waitingDao.existsByThemeId(id)) {
            throw new ConflictException("예약 대기가 존재하는 테마는 삭제할 수 없습니다.");
        }

        themeDao.delete(id);
    }

    public List<ReservationTimeDetailResult> findThemeSchedule(Long id, LocalDate date) {
        List<TimeQueryResult> availableTimes = themeDao.findTimeStatusBy(id, date);

        return availableTimes.stream()
                .map(ReservationTimeDetailResult::from)
                .toList();
    }

    private Theme getThemeOrThrow(Long id) {
        return themeDao.findThemeById(id).orElseThrow(
                () -> new NotFoundException("존재하지 않는 테마입니다."));
    }

    private Store getManagedStoreOrThrow(Long memberId) {
        return storeDao.findByManagerMemberId(memberId)
                .orElseThrow(() -> new ForbiddenException("매장 관리 권한이 없습니다."));
    }
}