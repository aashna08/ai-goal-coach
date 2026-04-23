package com.example.goalcoach.testsupport;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TestServerConfig {
    protected GoalCoachTestServer server;
    protected int port;

    @BeforeAll
    void startServer() throws Exception {
        server = new GoalCoachTestServer();
        server.start();
        port = server.port();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @AfterAll
    void stopServer() {
        if (server != null) server.stop();
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}

