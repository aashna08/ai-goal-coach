package com.example.goalcoach.repro;

import com.example.goalcoach.testsupport.GoalCoachTestServer;

import java.io.IOException;

/**
 * Starts the same embedded HTTP server used in JUnit tests, so you can manually hit
 * {@code /api/goal-coach} while reading {@code reproducible-bugs/}.
 *
 * <p>Run from repo root:</p>
 * <pre>
 * mvn -pl tests exec:java -Dexec.classpathScope=test \
 *   -Dexec.mainClass=com.example.goalcoach.repro.StandaloneGoalCoachServer
 * </pre>
 */
public final class StandaloneGoalCoachServer {
    private StandaloneGoalCoachServer() {}

    public static void main(String[] args) throws IOException {
        GoalCoachTestServer server = new GoalCoachTestServer();
        server.start();
        int port = server.port();
        String base = "http://127.0.0.1:" + port;
        System.out.println("Standalone Goal Coach server (stub/Ollama per config):");
        System.out.println("  Base URL: " + base);
        System.out.println();
        System.out.println("Example (RestAssured repro checks, terminal 2):");
        System.out.println("  mvn -q -pl tests exec:java -Dexec.classpathScope=test \\");
        System.out.println("    -Dexec.mainClass=com.example.goalcoach.repro.ReproBugHttpChecks \\");
        System.out.println("    -Drepro.baseUrl=" + base);
        System.out.println();
        System.out.println("Press Enter to stop…");
        try {
            System.in.read();
        } finally {
            server.stop();
        }
    }
}
