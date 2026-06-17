CREATE TABLE IF NOT EXISTS member
(
    id       BIGINT      AUTO_INCREMENT PRIMARY KEY,
    email    VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name     VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS store
(
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    manager_member_id BIGINT       NOT NULL,
    FOREIGN KEY (manager_member_id) REFERENCES member (id)
);

CREATE TABLE IF NOT EXISTS reservation_time
(
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    start_at VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS theme
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    url         VARCHAR(255) NOT NULL,
    store_id    BIGINT,
    PRIMARY KEY (id),
    FOREIGN KEY (store_id) REFERENCES store (id)
);

CREATE TABLE IF NOT EXISTS reservation
(
    id        BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    date      DATE   NOT NULL,
    time_id   BIGINT NOT NULL,
    theme_id  BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_reservation_date_time_theme UNIQUE (date, time_id, theme_id),
    FOREIGN KEY (member_id) REFERENCES member (id),
    FOREIGN KEY (time_id) REFERENCES reservation_time (id),
    FOREIGN KEY (theme_id) REFERENCES theme (id)
);

CREATE TABLE IF NOT EXISTS waiting
(
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    member_id  BIGINT   NOT NULL,
    date       DATE     NOT NULL,
    time_id    BIGINT   NOT NULL,
    theme_id   BIGINT   NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_waiting_member_date_time_theme UNIQUE (member_id, date, time_id, theme_id),
    FOREIGN KEY (member_id) REFERENCES member (id),
    FOREIGN KEY (time_id) REFERENCES reservation_time (id),
    FOREIGN KEY (theme_id) REFERENCES theme (id)
)
