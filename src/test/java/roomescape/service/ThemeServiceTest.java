package roomescape.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static roomescape.config.FixedClockConfig.TODAY;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import roomescape.common.exception.ConflictException;
import roomescape.common.exception.ForbiddenException;
import roomescape.config.FixedClockConfig;
import roomescape.dao.ReservationDao;
import roomescape.dao.StoreDao;
import roomescape.dao.ThemeDao;
import roomescape.dao.WaitingDao;
import roomescape.domain.reservation.theme.Description;
import roomescape.domain.reservation.theme.Theme;
import roomescape.domain.reservation.theme.ThemeName;
import roomescape.domain.reservation.theme.ThumbnailUrl;
import roomescape.domain.store.Store;
import roomescape.service.dto.command.ThemeCommand;
import roomescape.service.dto.result.ThemeResult;

@ExtendWith(MockitoExtension.class)
class ThemeServiceTest {
    private final Long themeId = 1L;
    private final Long managerId = 1L;
    private final Long storeId = 1L;
    private final String themeNameValue = "저주받은 저택";
    private final String descriptionValue = "100년 전 사라진 가문의 비밀을 파헤쳐라";

    private final Clock fixedClock = new FixedClockConfig().testClock();
    private final Store store = new Store(storeId, "방탈출 강남점", managerId);

    private ThemeService themeService;

    @Mock
    private ThemeDao themeDao;
    @Mock
    private ReservationDao reservationDao;

    @Mock
    private WaitingDao waitingDao;

    @Mock
    private StoreDao storeDao;

    @Mock
    private MultipartFile file;

    @BeforeEach
    public void setUp() {
        themeService = new ThemeService(themeDao, waitingDao, reservationDao, storeDao, fixedClock);
    }

    @Test
    public void 테마_생성_정상_테스트() {
        given(storeDao.findByManagerMemberId(managerId)).willReturn(Optional.of(store));
        given(themeDao.existsByName(themeNameValue)).willReturn(false);
        given(file.getOriginalFilename()).willReturn("cursed.jpg");
        Theme saved = new Theme(
                themeId,
                ThemeName.parse(themeNameValue),
                Description.parse(descriptionValue),
                ThumbnailUrl.parse("/images/uuid_cursed.jpg"),
                storeId
        );
        given(themeDao.save(any())).willReturn(saved);

        ThemeCommand command = new ThemeCommand(themeNameValue, descriptionValue, file);
        ThemeResult result = themeService.createTheme(managerId, command);

        assertThat(result.id()).isEqualTo(saved.getId());
        assertThat(result.name()).isEqualTo(saved.getName().value());
        assertThat(result.description()).isEqualTo(saved.getDescription().value());
        assertThat(result.url()).isEqualTo(saved.getUrl().value());

        verify(themeDao).save(argThat(theme ->
                theme.getName().value().equals(themeNameValue)
                        && theme.getDescription().value().equals(descriptionValue)
                        && theme.getUrl().value().endsWith("_cursed.jpg")
                        && theme.getStoreId().equals(storeId)
        ));
    }

    @Test
    public void 중복된_이름의_테마_생성_시_예외_테스트() {
        given(storeDao.findByManagerMemberId(managerId)).willReturn(Optional.of(store));
        given(themeDao.existsByName(themeNameValue)).willReturn(true);

        ThemeCommand command = new ThemeCommand(themeNameValue, descriptionValue, file);

        assertThatThrownBy(() -> themeService.createTheme(managerId, command))
                .isInstanceOf(ConflictException.class)
                .hasMessage("이미 존재하는 테마입니다.");

        verify(themeDao, never()).save(any());
    }

    @Test
    public void 매니저가_아니면_테마_생성_시_예외_테스트() {
        given(storeDao.findByManagerMemberId(managerId)).willReturn(Optional.empty());

        ThemeCommand command = new ThemeCommand(themeNameValue, descriptionValue, file);

        assertThatThrownBy(() -> themeService.createTheme(managerId, command))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("매장 관리 권한이 없습니다.");

        verify(themeDao, never()).save(any());
    }

    @Test
    public void 인기_테마_조회_정상_테스트() {
        Long count = 10L;
        given(themeDao.findTopThemes(count, LocalDate.parse(TODAY))).willReturn(List.of());

        themeService.findTopTheme(count);

        verify(themeDao).findTopThemes(count, LocalDate.parse(TODAY));
    }

    @Test
    public void 테마_삭제_정상_테스트() {
        Theme existing = ownStoreTheme();
        given(storeDao.findByManagerMemberId(managerId)).willReturn(Optional.of(store));
        given(themeDao.findThemeById(themeId)).willReturn(Optional.of(existing));
        given(reservationDao.existsByThemeId(themeId)).willReturn(false);
        given(waitingDao.existsByThemeId(themeId)).willReturn(false);
        themeService.deleteTheme(managerId, themeId);

        verify(themeDao).delete(themeId);
    }

    @Test
    public void 다른_매장의_테마_삭제_시_예외_테스트() {
        Theme otherStoreTheme = new Theme(
                themeId,
                ThemeName.parse(themeNameValue),
                Description.parse(descriptionValue),
                ThumbnailUrl.parse("/images/cursed.jpg"),
                storeId + 1
        );
        given(storeDao.findByManagerMemberId(managerId)).willReturn(Optional.of(store));
        given(themeDao.findThemeById(themeId)).willReturn(Optional.of(otherStoreTheme));

        assertThatThrownBy(() -> themeService.deleteTheme(managerId, themeId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("다른 매장의 테마는 관리할 수 없습니다.");

        verify(themeDao, never()).delete(themeId);
    }

    @Test
    public void 예약이_존재하는_테마_삭제_시_예외_테스트() {
        Theme existing = ownStoreTheme();
        given(storeDao.findByManagerMemberId(managerId)).willReturn(Optional.of(store));
        given(themeDao.findThemeById(themeId)).willReturn(Optional.of(existing));
        given(reservationDao.existsByThemeId(themeId)).willReturn(true);

        assertThatThrownBy(() -> themeService.deleteTheme(managerId, themeId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("예약이 존재하는 테마는 삭제할 수 없습니다.");

        verify(themeDao, never()).delete(themeId);
    }

    @Test
    public void 예약_대기가_존재하는_테마_삭제_시_예외_테스트() {
        Theme existing = ownStoreTheme();
        given(storeDao.findByManagerMemberId(managerId)).willReturn(Optional.of(store));
        given(themeDao.findThemeById(themeId)).willReturn(Optional.of(existing));
        given(reservationDao.existsByThemeId(themeId)).willReturn(false);
        given(waitingDao.existsByThemeId(themeId)).willReturn(true);

        assertThatThrownBy(() -> themeService.deleteTheme(managerId, themeId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("예약 대기가 존재하는 테마는 삭제할 수 없습니다.");

        verify(themeDao, never()).delete(themeId);
    }

    private Theme ownStoreTheme() {
        return new Theme(
                themeId,
                ThemeName.parse(themeNameValue),
                Description.parse(descriptionValue),
                ThumbnailUrl.parse("/images/cursed.jpg"),
                storeId
        );
    }
}