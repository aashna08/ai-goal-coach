package com.example.goalcoach.engine;

import com.example.goalcoach.config.GoalCoachRuntimeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Locale;

/**
 * Selects which {@link GoalCoachEngine} implementation the embedded test server should use.
 *
 * <p>Primary selector:</p>
 * <ul>
 *   <li>{@code goalcoach.engine} / {@code GOALCOACH_ENGINE}: {@code stub} (default) or {@code ollama}</li>
 * </ul>
 *
 * <p>Ollama configuration:</p>
 * <ul>
 *   <li>{@code goalcoach.ollama.baseUrl} / {@code GOALCOACH_OLLAMA_BASE_URL} (default {@code http://localhost:11434})</li>
 *   <li>{@code goalcoach.ollama.model} / {@code GOALCOACH_OLLAMA_MODEL} (example: {@code llama3.1})</li>
 *   <li>{@code goalcoach.ollama.connectTimeoutMs} / {@code GOALCOACH_OLLAMA_CONNECT_TIMEOUT_MS} (default 2000)</li>
 *   <li>{@code goalcoach.ollama.requestTimeoutMs} / {@code GOALCOACH_OLLAMA_REQUEST_TIMEOUT_MS} (default 300000; thinking models are often slower)</li>
 * </ul>
 *
 * <p>Shared defaults also live in classpath {@code goalcoach.properties}; see {@link GoalCoachRuntimeConfig}.</p>
 */
public final class GoalCoachEngines {
    private GoalCoachEngines() {}

    public static GoalCoachEngine create(ObjectMapper mapper) {
       // GoalCoachRuntimeConfig cfg = GoalCoachRuntimeConfig.get();
        String engine = GoalCoachRuntimeConfig.resolve("goalcoach.engine", "GOALCOACH_ENGINE", "stub").trim().toLowerCase(Locale.ROOT);

        if ("ollama".equals(engine)) {
            String baseUrl = GoalCoachRuntimeConfig.resolve("goalcoach.ollama.baseUrl", "GOALCOACH_OLLAMA_BASE_URL", "http://localhost:11434");
            String model = GoalCoachRuntimeConfig.resolve("goalcoach.ollama.model", "GOALCOACH_OLLAMA_MODEL", "");

            Duration connect = Duration.ofMillis(parseInt(
                    GoalCoachRuntimeConfig.resolve("goalcoach.ollama.connectTimeoutMs", "GOALCOACH_OLLAMA_CONNECT_TIMEOUT_MS", "2000"),
                    2000
            ));
            Duration request = Duration.ofMillis(parseInt(
                    GoalCoachRuntimeConfig.resolve("goalcoach.ollama.requestTimeoutMs", "GOALCOACH_OLLAMA_REQUEST_TIMEOUT_MS", "300000"),
                    300_000
            ));

            return new OllamaGoalCoachEngine(mapper, baseUrl, model, connect, request);
        }

        return new StubGoalCoachEngine();
    }

    private static int parseInt(String raw, int defaultValue) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
