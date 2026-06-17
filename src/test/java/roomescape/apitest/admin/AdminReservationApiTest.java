package roomescape.apitest.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static roomescape.config.FixedClockConfig.FUTURE_DATE;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AdminReservationApiTest {

    @Test
    @DisplayName("매니저는 자기 매장(테마 1~8)의 예약만 조회한다.")
    void 매니저_자기_매장_예약만_조회() {
        String token = loginAs("브라운"); // 강남점 매니저

        List<Integer> themeIds = RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .when().get("/admin/reservations")
                .then().log().all()
                .statusCode(200)
                .extract().jsonPath().getList("themeResponse.id", Integer.class);

        assertThat(themeIds).isNotEmpty();
        assertThat(themeIds).allMatch(id -> id <= 8);
    }

    @Test
    @DisplayName("매니저는 자기 매장의 예약을 삭제할 수 있다(소유자가 아니어도).")
    void 매니저_자기_매장_예약_삭제() {
        String token = loginAs("브라운");
        long ownStoreReservationId = 3L; // 테마1(강남점), 소유자는 토리

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .when().delete("/admin/reservations/" + ownStoreReservationId)
                .then().log().all()
                .statusCode(204);
    }

    @Test
    @DisplayName("매니저는 자기 매장의 예약을 변경할 수 있다.")
    void 매니저_자기_매장_예약_변경() {
        String token = loginAs("브라운");
        long ownStoreReservationId = 25L; // 2026-05-12, 테마1(강남점)

        Map<String, Object> body = new HashMap<>();
        body.put("date", FUTURE_DATE);
        body.put("timeId", 1L);
        body.put("themeId", 1L);

        String updatedDate = RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .when().put("/admin/reservations/" + ownStoreReservationId)
                .then().log().all()
                .statusCode(200)
                .extract().jsonPath().getString("date");

        assertThat(updatedDate).isEqualTo(FUTURE_DATE);
    }

    @Test
    @DisplayName("매니저는 다른 매장의 예약을 삭제할 수 없다(소유 예약이라도).")
    void 매니저_타_매장_예약_삭제_불가() {
        String token = loginAs("브라운");
        long otherStoreReservationId = 1L; // 테마11(잠실점), 소유자는 브라운

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .when().delete("/admin/reservations/" + otherStoreReservationId)
                .then().log().all()
                .statusCode(403)
                .body("message", containsString("다른 매장의 예약은 관리할 수 없습니다."));
    }

    @Test
    @DisplayName("매니저가 아닌 사용자는 매장 예약 관리에 접근할 수 없다(403).")
    void 비매니저_접근_불가() {
        String token = loginAs("포비"); // 일반 사용자

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .when().get("/admin/reservations")
                .then().log().all()
                .statusCode(403)
                .body("message", containsString("매장 관리 권한이 없습니다."));
    }

    @Test
    @DisplayName("로그인하지 않으면 매장 예약 관리에 접근할 수 없다(401).")
    void 미로그인_접근_불가() {
        RestAssured.given().log().all()
                .when().get("/admin/reservations")
                .then().log().all()
                .statusCode(401);
    }

    private String loginAs(String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", emailOf(name));
        body.put("password", "password");

        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
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
}
