package roomescape.apitest.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static roomescape.config.FixedClockConfig.FUTURE_DATE;

import io.restassured.RestAssured;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationApiTest {
    private final String userName = "브라운";
    private final Long timeId = 1L;
    private final Long themeId = 1L;

    @Autowired
    JdbcTemplate jdbcTemplate;
    private int initialReservationSize;
    private int initialUserReservationSize;

    @BeforeEach
    void setUp() {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservation", Integer.class);
        initialReservationSize = Optional.ofNullable(total).orElse(0);

        Integer byUser = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservation r INNER JOIN member m ON r.member_id = m.id WHERE m.name = ?",
                Integer.class, userName);
        initialUserReservationSize = Optional.ofNullable(byUser).orElse(0);
    }

    @Test
    @DisplayName("예약을 생성하면 201과 생성된 id를 반환한다.")
    void 예약을_생성한다() {
        SessionFilter session = loginAs(userName);
        Long generatedId = createReservation(session, FUTURE_DATE, timeId, themeId);

        assertThat(generatedId).isNotNull();
    }

    @Test
    @DisplayName("생성한 예약이 전체 예약 조회에 포함된다.")
    void 생성한_예약이_전체_조회에_포함된다() {
        SessionFilter session = loginAs(userName);
        Long generatedId = createReservation(session, FUTURE_DATE, timeId, themeId);

        List<Long> allIds = getAllReservationIds(session);

        assertThat(allIds)
                .hasSize(initialReservationSize + 1)
                .contains(generatedId);
    }

    @Test
    @DisplayName("사용자 이름으로 조회하면 해당 사용자의 예약만 반환한다.")
    void 사용자_이름으로_본인_예약을_조회한다() {
        SessionFilter session = loginAs(userName);
        Long generatedId = createReservation(session, FUTURE_DATE, timeId, themeId);

        JsonPath body = getMyReservations(session);
        List<Long> ids = body.getList("reservationDetailResponses.id", Long.class);
        List<String> names = body.getList("reservationDetailResponses.name", String.class);

        assertThat(ids)
                .hasSize(initialUserReservationSize + 1)
                .contains(generatedId);
        assertThat(names).containsOnly(userName);
    }

    @Test
    @DisplayName("예약을 삭제하면 204를 반환하고 목록에서 제거된다.")
    void 예약을_삭제하면_목록에서_제거된다() {
        SessionFilter session = loginAs(userName);
        Long generatedId = createReservation(session, FUTURE_DATE, timeId, themeId);

        RestAssured.given().log().all()
                .filter(session)
                .when().delete("/reservations/" + generatedId)
                .then().log().all()
                .statusCode(204);

        List<Long> remainIds = getAllReservationIds(session);
        assertThat(remainIds)
                .hasSize(initialReservationSize)
                .doesNotContain(generatedId);
    }

    @Test
    void 본인_예약_취소_API() {
        long id = 23L;
        SessionFilter session = loginAs(userName);
        RestAssured.given().log().all()
                .filter(session)
                .when().delete("/reservations/" + id)
                .then().log().all()
                .statusCode(204);
    }

    @Test
    void 다른_사용자_예약_취소_API() {
        long id = 23L;
        SessionFilter session = loginAs("토리");
        RestAssured.given().log().all()
                .filter(session)
                .when().delete("/reservations/" + id)
                .then().log().all()
                .statusCode(403)
                .body("message", is("다른 사람의 예약은 취소/변경할 수 없습니다."));
    }

    @Test
    void 예약_사용자_시간_변경_API() {
        long id = 24L;
        Long timeId = 2L;
        SessionFilter session = loginAs(userName);
        Map<String, Object> reservation = new HashMap<>();
        reservation.put("date", FUTURE_DATE);
        reservation.put("timeId", timeId);
        reservation.put("themeId", themeId);

        Long updatedTimeId = RestAssured.given().log().all()
                .filter(session)
                .contentType(ContentType.JSON)
                .body(reservation)
                .when().put("/reservations/" + id)
                .then().log().all()
                .statusCode(200)
                .extract().jsonPath().getLong("timeResponse.id");

        assertThat(updatedTimeId).isEqualTo(timeId);
    }

    @Test
    void 예약_사용자_날짜_변경_API() {
        long id = 24L;
        String date = "2026-05-13";
        SessionFilter session = loginAs(userName);
        Map<String, Object> reservation = new HashMap<>();
        reservation.put("date", date);
        reservation.put("timeId", timeId);
        reservation.put("themeId", themeId);

        String updatedDate = RestAssured.given().log().all()
                .filter(session)
                .contentType(ContentType.JSON)
                .body(reservation)
                .when().put("/reservations/" + id)
                .then().log().all()
                .statusCode(200)
                .extract().jsonPath().getString("date");

        assertThat(updatedDate).isEqualTo(date);
    }

    @Test
    void 예약과_예약_대기_조회_API() {
        SessionFilter session = loginAs("토리");
        JsonPath body = getMyReservations(session);
        List<Map<String, Object>> details = body.getList("reservationDetailResponses");

        long reservedCount = details.stream()
                .filter(r -> r.get("status").equals("RESERVED"))
                .count();
        long waitingCount = details.stream()
                .filter(r -> r.get("status").equals("WAITING"))
                .count();

        assertThat(reservedCount).isEqualTo(5);
        assertThat(waitingCount).isEqualTo(1);
        List<String> dates = body.getList("reservationDetailResponses.date");
        assertThat(dates).isSortedAccordingTo(Comparator.naturalOrder());
    }

    @Test
    @DisplayName("예약 날짜가 null이면 상태코드 400을 반환한다.")
    void 요청_날짜_null_테스트() {
        SessionFilter session = loginAs(userName);
        Map<String, Object> params = new HashMap<>();
        params.put("timeId", timeId);
        params.put("themeId", themeId);

        RestAssured.given().log().all()
                .filter(session)
                .contentType(ContentType.JSON)
                .body(params)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("예약 날짜의 형식이 올바르지 않으면 상태코드 400을 반환한다.")
    void 요청_날짜_형식_불일치_테스트() {
        SessionFilter session = loginAs(userName);
        Map<String, Object> params = new HashMap<>();
        params.put("date", "26-01-01");
        params.put("timeId", timeId);
        params.put("themeId", themeId);

        RestAssured.given().log().all()
                .filter(session)
                .contentType(ContentType.JSON)
                .body(params)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("시간 식별자가 null이면 상태코드 400을 반환한다.")
    void 요청_시간_식별자_null_테스트() {
        SessionFilter session = loginAs(userName);
        Map<String, Object> params = new HashMap<>();
        params.put("date", FUTURE_DATE);
        params.put("themeId", themeId);

        RestAssured.given().log().all()
                .filter(session)
                .contentType(ContentType.JSON)
                .body(params)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("테마 식별자가 null이면 상태코드 400을 반환한다.")
    void 요청_테마_식별자_null_테스트() {
        SessionFilter session = loginAs(userName);
        Map<String, Object> params = new HashMap<>();
        params.put("date", FUTURE_DATE);
        params.put("timeId", timeId);

        RestAssured.given().log().all()
                .filter(session)
                .contentType(ContentType.JSON)
                .body(params)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    private SessionFilter loginAs(String name) {
        SessionFilter session = new SessionFilter();
        Map<String, Object> body = new HashMap<>();
        body.put("email", emailOf(name));
        body.put("password", "password");

        RestAssured.given()
                .filter(session)
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/login")
                .then().statusCode(200);

        return session;
    }

    private String emailOf(String name) {
        return switch (name) {
            case "브라운" -> "brown@woowa.com";
            case "토리" -> "tory@woowa.com";
            case "포비" -> "pobi@woowa.com";
            case "로운" -> "roun@woowa.com";
            default -> throw new IllegalArgumentException("알 수 없는 사용자: " + name);
        };
    }

    private Long createReservation(SessionFilter session, String date, Long timeId, Long themeId) {
        Map<String, Object> body = new HashMap<>();
        body.put("date", date);
        body.put("timeId", timeId);
        body.put("themeId", themeId);

        return RestAssured.given().log().all()
                .filter(session)
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private List<Long> getAllReservationIds(SessionFilter session) {
        return RestAssured.given().log().all()
                .filter(session)
                .when().get("/reservations")
                .then().log().all()
                .statusCode(200)
                .extract().jsonPath().getList("id", Long.class);
    }

    private JsonPath getMyReservations(SessionFilter session) {
        return RestAssured.given().log().all()
                .filter(session)
                .when().get("/reservations/mine")
                .then().log().all()
                .statusCode(200)
                .extract().jsonPath();
    }
}
