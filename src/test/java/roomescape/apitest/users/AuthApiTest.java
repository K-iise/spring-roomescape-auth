package roomescape.apitest.users;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

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
class AuthApiTest {

    @Test
    @DisplayName("로그인한 매니저는 자신의 관리 매장 정보를 조회한다.")
    void 매니저_관리_매장_조회() {
        String token = loginAs("brown@woowa.com");

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .when().get("/login/check")
                .then().log().all()
                .statusCode(200)
                .body("name", equalTo("브라운"))
                .body("store.id", equalTo(1))
                .body("store.name", equalTo("방탈출 강남점"));
    }

    @Test
    @DisplayName("로그인한 일반 사용자는 관리 매장 정보가 없다.")
    void 일반_사용자_관리_매장_없음() {
        String token = loginAs("pobi@woowa.com");

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .when().get("/login/check")
                .then().log().all()
                .statusCode(200)
                .body("name", equalTo("포비"))
                .body("store", nullValue());
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
