package com.example.goalcoach.repro;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Manual repro checks for {@code BUGS.md} / {@code reproducible-bugs/} using the same stack as
 * {@link com.example.goalcoach.GoalCoachApiContractTest}: <b>RestAssured</b> (no shell/Python).
 *
 * <p>Start {@link StandaloneGoalCoachServer}, then from repo root:</p>
 * <pre>
 * mvn -q -pl tests exec:java -Dexec.classpathScope=test \
 *   -Dexec.mainClass=com.example.goalcoach.repro.ReproBugHttpChecks \
 *   -Drepro.baseUrl=http://127.0.0.1:PORT
 * </pre>
 *
 * <p>Or: {@code export REPRO_BASE_URL=http://127.0.0.1:PORT} and omit {@code -Drepro.baseUrl}.</p>
 */
public final class ReproBugHttpChecks {
    private ReproBugHttpChecks() {}

    public static void main(String[] args) {
        String base = firstNonBlank(System.getenv("REPRO_BASE_URL"), System.getProperty("repro.baseUrl", ""));
        if (base.isBlank()) {
            System.err.println("Set REPRO_BASE_URL or -Drepro.baseUrl=http://127.0.0.1:<port> (from StandaloneGoalCoachServer).");
            System.exit(2);
        }
        applyBaseUrl(base.trim());

        System.out.println("Repro checks against: " + base);
        checkBug01GibberishGuardrail();
        System.out.println("  OK — Bug 01 (gibberish hallucination guardrail)");
        checkBug02ContractShape();
        System.out.println("  OK — Bug 02 (contract shape / types)");
        checkBug03NoExtraFields();
        System.out.println("  OK — Bug 03 (no extra top-level fields)");
        System.out.println("All reproducible-bug HTTP checks passed.");
    }

    private static void applyBaseUrl(String baseUrl) {
        URI u = URI.create(baseUrl);
        String scheme = u.getScheme() == null ? "http" : u.getScheme();
        String host = u.getHost() == null ? "127.0.0.1" : u.getHost();
        int port = u.getPort();
        if (port < 0) {
            port = "https".equalsIgnoreCase(scheme) ? 443 : 80;
        }
        RestAssured.baseURI = scheme + "://" + host;
        RestAssured.port = port;
        RestAssured.basePath = "";
    }

    private static void checkBug01GibberishGuardrail() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"goal\":\"asdlkjasd qwwerty zzzzzz 9999 !!!!\"}")
        .when()
                .post("/api/goal-coach")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("confidence_score", lessThanOrEqualTo(2))
                .body("refined_goal", is(""))
                .body("key_results.size()", is(0));
    }

    private static void checkBug02ContractShape() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"goal\":\"I want to improve in sales\"}")
        .when()
                .post("/api/goal-coach")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(matchesJsonSchemaInClasspath("schema/goal_coach_response.schema.json"))
                .body("refined_goal", notNullValue())
                .body("key_results", notNullValue())
                .body("confidence_score", allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(10)))
                .body("confidence_score", instanceOf(Integer.class));
    }

    private static void checkBug03NoExtraFields() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"goal\":\"Improve public speaking\"}")
        .when()
                .post("/api/goal-coach")
        .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schema/goal_coach_response.schema.json"));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a.trim();
        }
        if (b != null && !b.trim().isEmpty()) {
            return b.trim();
        }
        return "";
    }
}
