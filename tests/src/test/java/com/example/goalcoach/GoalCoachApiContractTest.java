package com.example.goalcoach;

import com.example.goalcoach.config.GoalCoachRuntimeConfig;
import com.example.goalcoach.testsupport.TestServerConfig;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

public class GoalCoachApiContractTest extends TestServerConfig {

    @Test
    void returnsStrictSchemaAndRequestIdHeader() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"goal\":\"I want to improve in sales\"}")
        .when()
                .post("/api/goal-coach")
        .then()
                .statusCode(200)
                .header("X-Request-Id", not(isEmptyOrNullString()))
                .contentType(ContentType.JSON)
                .body(matchesJsonSchemaInClasspath("schema/goal_coach_response.schema.json"))
                .body("refined_goal", notNullValue())
                .body("key_results", notNullValue())
                .body("confidence_score", allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(10)));
    }

    @Test
    void confidenceHighMeansNonEmptyRefinedGoalAnd3To5KeyResults() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"goal\":\"I want to improve in sales\"}")
        .when()
                .post("/api/goal-coach")
        .then()
                .statusCode(200)
                .body("confidence_score", greaterThanOrEqualTo(6))
                .body("refined_goal", not(isEmptyOrNullString()))
                .body("key_results.size()", allOf(greaterThanOrEqualTo(3), lessThanOrEqualTo(5)));
    }

    @Test
    void emptyGoalGetsLowConfidenceAndNoHallucinatedOutputs() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"goal\":\"   \"}")
        .when()
                .post("/api/goal-coach")
        .then()
                .statusCode(200)
                .body("confidence_score", lessThanOrEqualTo(2))
                .body("refined_goal", anyOf(isEmptyString(), is("")))
                .body("key_results.size()", is(0));
    }

    @Test
    void adversarialSqlInjectionGetsLowConfidenceAndNoHallucination() {
        String payload = "I want to improve in sales; DROP TABLE users; --";
        given()
                .contentType(ContentType.JSON)
                .body("{\"goal\":\"" + payload.replace("\"", "\\\"") + "\"}")
        .when()
                .post("/api/goal-coach")
        .then()
                .statusCode(200)
                .body("confidence_score", lessThanOrEqualTo(2))
                .body("refined_goal", anyOf(isEmptyString(), is("")))
                .body("key_results.size()", is(0));
    }

    @Test
    void profanityGetsLowConfidenceAndNoHallucination() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"goal\":\"I want to be a better manager you asshole\"}")
        .when()
                .post("/api/goal-coach")
        .then()
                .statusCode(200)
                .body("confidence_score", lessThanOrEqualTo(2))
                .body("refined_goal", anyOf(isEmptyString(), is("")))
                .body("key_results.size()", is(0));
    }

    @Test
    void gibberishGetsLowConfidenceAndNoHallucination() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"goal\":\"asdlkjasd qwwerty zzzzzz 9999 !!!!\"}")
        .when()
                .post("/api/goal-coach")
        .then()
                .statusCode(200)
                .body("confidence_score", lessThanOrEqualTo(2))
                .body("refined_goal", anyOf(isEmptyString(), is("")))
                .body("key_results.size()", is(0));
    }

    @Test
    void doesNotAllowAdditionalFieldsInResponse() {

        given()
                .contentType(ContentType.JSON)
                .body("{\"goal\":\"Improve public speaking\"}")
        .when()
                .post("/api/goal-coach")
        .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schema/goal_coach_response.schema.json"));
    }

    @Test
    void basicPerformanceP95Under250msForStub() {
        Assumptions.assumeTrue(
                "stub".equalsIgnoreCase(
                        GoalCoachRuntimeConfig.resolve("goalcoach.engine", "GOALCOACH_ENGINE", "stub").trim()
                ),
                () -> "p95 < 250ms is only meaningful for the deterministic stub engine"
        );
        int n = 50;
        long[] times = new long[n];
        for (int i = 0; i < n; i++) {
            long start = System.nanoTime();
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"goal\":\"I want to improve in sales\"}")
            .when()
                    .post("/api/goal-coach")
            .then()
                    .statusCode(200)
                    .body("confidence_score", greaterThanOrEqualTo(6));
            times[i] = (System.nanoTime() - start) / 1_000_000;
        }
        java.util.Arrays.sort(times);
        long p95 = times[(int) Math.floor(0.95 * (n - 1))];
        org.junit.jupiter.api.Assertions.assertTrue(
                p95 < 250,
                "Expected p95 < 250ms for stub, got " + p95 + "ms"
        );
    }
}

