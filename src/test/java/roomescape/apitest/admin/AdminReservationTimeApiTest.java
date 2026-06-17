package roomescape.apitest.admin;

import static org.hamcrest.Matchers.containsString;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AdminReservationTimeApiTest {

    @Test
    void 시간_관리자_API() {
        String token = loginAsManager();
        Map<String, String> params = new HashMap<>();
        params.put("startAt", "19:00");

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(params)
                .when().post("/admin/times")
                .then().log().all()
                .statusCode(201);

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .when().delete("/admin/times/10")
                .then().log().all()
                .statusCode(204);
    }

    @Test
    @DisplayName("테마 이름이 null이면 상태코드 400을 반환한다.")
    void 요청_이름_null_테스트() {
        String token = loginAsManager();
        Map<String, String> params = new HashMap<>();

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(params)
                .when().post("/admin/times")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("매니저가 아닌 사용자는 시간 관리에 접근할 수 없다(403).")
    void 비매니저_접근_불가() {
        String token = loginAs("pobi@woowa.com"); // 일반 사용자

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .when().delete("/admin/times/10")
                .then().log().all()
                .statusCode(403)
                .body("message", containsString("매장 관리 권한이 없습니다."));
    }

    private String loginAsManager() {
        return loginAs("brown@woowa.com"); // 강남점 매니저
    }

    private String loginAs(String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", "password");

        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }
}
