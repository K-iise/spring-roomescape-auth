package roomescape.domain.reservation.theme;

import java.util.Objects;

public class Theme {
    private final Long id;
    private final ThemeName name;
    private final Description description;
    private final ThumbnailUrl url;
    private final Long storeId;

    public Theme(Long id, ThemeName name, Description description, ThumbnailUrl url) {
        this(id, name, description, url, null);
    }

    public Theme(Long id, ThemeName name, Description description, ThumbnailUrl url, Long storeId) {
        validate(name, description, url);
        this.id = id;
        this.name = name;
        this.description = description;
        this.url = url;
        this.storeId = storeId;
    }

    private void validate(ThemeName name, Description description, ThumbnailUrl url) {
        Objects.requireNonNull(name, "테마 이름이 비어 있습니다.");
        Objects.requireNonNull(description, "테마 설명이 비어 있습니다.");
        Objects.requireNonNull(url, "테마 썸네일 주소가 비어 있습니다.");
    }

    public Long getId() {
        return id;
    }

    public ThemeName getName() {
        return name;
    }

    public Description getDescription() {
        return description;
    }

    public ThumbnailUrl getUrl() {
        return url;
    }

    public Long getStoreId() {
        return storeId;
    }
}
