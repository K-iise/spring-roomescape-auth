package roomescape.apitest.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

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
class AdminThemeApiTest {

    @Test
    void 테마_관리_API() {
        String token = loginAsManager();
        String name = "추리물";
        String description = "추리";
        byte[] fileContent = "fake-image-content".getBytes();

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.MULTIPART)
                .multiPart("name", name)
                .multiPart("description", description)
                .multiPart("file", "test.png", fileContent, "image/png")
                .when().post("/admin/themes")
                .then().log().all()
                .statusCode(201);

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .when().delete("/admin/themes/16")
                .then().log().all()
                .statusCode(204);
    }

    @Test
    @DisplayName("테마 이름이 null이면 상태코드 400을 반환한다.")
    void 요청_이름_null_테스트() {
        String token = loginAsManager();
        String description = "추리";
        byte[] fileContent = "fake-image-content".getBytes();

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .contentType(ContentType.MULTIPART)
                .multiPart("description", description)
                .multiPart("file", "test.png", fileContent, "image/png")
                .when().post("/admin/themes")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("테마 설명이 null이면 상태코드 400을 반환한다.")
    void 요청_설명_null_테스트() {
        String token = loginAsManager();
        String name = "추리물";
        byte[] fileContent = "fake-image-content".getBytes();

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .contentType(ContentType.MULTIPART)
                .multiPart("name", name)
                .multiPart("file", "test.png", fileContent, "image/png")
                .when().post("/admin/themes")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("테마 썸네일이 null이면 상태코드 400을 반환한다.")
    void 요청_썸네일_null_테스트() {
        String token = loginAsManager();
        String name = "추리물";
        String description = "추리";

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .contentType(ContentType.MULTIPART)
                .multiPart("name", name)
                .multiPart("description", description)
                .when().post("/admin/themes")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("매니저는 자기 매장의 테마만 조회한다(강남점: 테마 1~8).")
    void 매니저_자기_매장_테마만_조회() {
        String token = loginAsManager(); // 강남점 매니저

        List<Integer> themeIds = RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .when().get("/admin/themes")
                .then().log().all()
                .statusCode(200)
                .extract().jsonPath().getList("id", Integer.class);

        assertThat(themeIds).isNotEmpty();
        assertThat(themeIds).allMatch(id -> id <= 8);
    }

    @Test
    @DisplayName("매니저는 다른 매장의 테마를 삭제할 수 없다(403).")
    void 매니저_타_매장_테마_삭제_불가() {
        String token = loginAsManager(); // 강남점 매니저
        long otherStoreThemeId = 9L; // 잠실점 테마

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .when().delete("/admin/themes/" + otherStoreThemeId)
                .then().log().all()
                .statusCode(403)
                .body("message", containsString("다른 매장의 테마는 관리할 수 없습니다."));
    }

    @Test
    @DisplayName("로그인하지 않으면 테마 관리에 접근할 수 없다(401).")
    void 미로그인_접근_불가() {
        RestAssured.given().log().all()
                .when().delete("/admin/themes/16")
                .then().log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("매니저가 아닌 사용자는 테마 관리에 접근할 수 없다(403).")
    void 비매니저_접근_불가() {
        String token = loginAs("pobi@woowa.com"); // 일반 사용자

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .when().delete("/admin/themes/16")
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
