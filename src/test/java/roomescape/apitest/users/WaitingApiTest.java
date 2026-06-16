package roomescape.apitest.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static roomescape.config.FixedClockConfig.FUTURE_DATE;

import io.restassured.RestAssured;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WaitingApiTest {

    private static final Long timeId = 5L;
    private static final Long themeId = 1L;
    private static final Long waitingId = 1L;

    @Test
    void 예약_대기_생성_API() {
        SessionFilter session = loginAs("로운");
        Long createdId = createWaiting(session, FUTURE_DATE, timeId, themeId);
        assertThat(createdId).isNotNull();

        JsonPath jsonPath = getMyReservations(session);
        List<Map<String, Object>> details = jsonPath.getList("reservationDetailResponses");

        assertThat(details).hasSize(1);
        Map<String, Object> only = details.getFirst();
        assertThat(only.get("status")).isEqualTo("WAITING");
        assertThat(only.get("sequence")).isEqualTo(2);
        assertThat(only.get("date")).isEqualTo(FUTURE_DATE);
    }

    @Test
    void 예약이_없는_슬롯에_대기를_신청하면_예외가_발생한다() {
        SessionFilter session = loginAs("로운");
        Long emptySlotTimeId = 6L;

        Map<String, Object> body = new HashMap<>();
        body.put("date", FUTURE_DATE);
        body.put("timeId", emptySlotTimeId);
        body.put("themeId", themeId);

        RestAssured.given().log().all()
                .filter(session)
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/waitings")
                .then().log().all()
                .statusCode(422)
                .body("message", containsString("예약이 존재하지 않으면 예약 대기를 생성할 수 없습니다."));

        assertThat(getMyReservations(session).getList("reservationDetailResponses")).isEmpty();
    }

    @Test
    void 같은_사용자가_같은_슬롯에_중복으로_대기를_신청하면_예외가_발생한다() {
        SessionFilter session = loginAs("토리");

        Map<String, Object> body = new HashMap<>();
        body.put("date", FUTURE_DATE);
        body.put("timeId", timeId);
        body.put("themeId", themeId);

        RestAssured.given().log().all()
                .filter(session)
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/waitings")
                .then().log().all()
                .statusCode(422)
                .body("message", containsString("예약 대기는 중복으로 생성할 수 없습니다."));

        assertThat(waitingCount(session)).isEqualTo(1);
    }

    @Test
    void 본인이_예약한_슬롯에_대기를_신청하면_예외가_발생한다() {
        SessionFilter session = loginAs("브라운");

        Map<String, Object> body = new HashMap<>();
        body.put("date", FUTURE_DATE);
        body.put("timeId", timeId);
        body.put("themeId", themeId);

        RestAssured.given().log().all()
                .filter(session)
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/waitings")
                .then().log().all()
                .statusCode(422)
                .body("message", containsString("본인이 이미 예약한 시간에는 대기를 신청할 수 없습니다."));

        assertThat(waitingCount(session)).isEqualTo(0);
    }

    @Test
    void 예약_대기_취소_API() {
        SessionFilter session = loginAs("토리");

        RestAssured.given().log().all()
                .filter(session)
                .when().delete("/waitings/" + waitingId)
                .then().log().all()
                .statusCode(204);

        assertThat(waitingCount(session)).isEqualTo(0);
    }

    @Test
    void 다른_사람의_대기를_삭제는_예외가_발생한다() {
        SessionFilter session = loginAs("로운");

        RestAssured.given().log().all()
                .filter(session)
                .when().delete("/waitings/" + waitingId)
                .then().log().all()
                .statusCode(403)
                .body("message", containsString("다른 사람의 예약 대기는 취소할 수 없습니다."));

        assertThat(waitingCount(loginAs("토리"))).isEqualTo(1);
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

    private Long createWaiting(SessionFilter session, String date, Long timeId, Long themeId) {
        Map<String, Object> body = new HashMap<>();
        body.put("date", date);
        body.put("timeId", timeId);
        body.put("themeId", themeId);

        return RestAssured.given().log().all()
                .filter(session)
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/waitings")
                .then().log().all()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private JsonPath getMyReservations(SessionFilter session) {
        return RestAssured.given().log().all()
                .filter(session)
                .when().get("/reservations/mine")
                .then().log().all()
                .statusCode(200)
                .extract().jsonPath();
    }

    private Long waitingCount(SessionFilter session) {
        List<Map<String, Object>> responses = getMyReservations(session)
                .getList("reservationDetailResponses");

        return responses.stream()
                .filter(r -> r.get("status").equals("WAITING"))
                .count();
    }
}
