package com.example.goalcoach.engine;

import com.example.goalcoach.model.GoalCoachResponse;
import com.example.goalcoach.safety.GoalInputSafety;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Goal coach backed by a local Ollama server using {@code POST /api/chat}.
 *
 * <p>Configuration is resolved via {@link com.example.goalcoach.config.GoalCoachRuntimeConfig} (see {@link GoalCoachEngines}).</p>
 * <ul>
 *   <li>{@code goalcoach.ollama.baseUrl} (default {@code http://localhost:11434})</li>
 *   <li>{@code goalcoach.ollama.model} (required for non-fallback behavior)</li>
 *   <li>{@code goalcoach.ollama.connectTimeoutMs} / {@code goalcoach.ollama.requestTimeoutMs}</li>
 * </ul>
 */
public final class OllamaGoalCoachEngine implements GoalCoachEngine {
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final URI chatUri;
    private final String model;
    private final Duration requestTimeout;

    public OllamaGoalCoachEngine(ObjectMapper mapper, String baseUrl, String model, Duration connectTimeout, Duration requestTimeout) {
        this.mapper = mapper;
        this.model = model;
        this.requestTimeout = requestTimeout;
        this.chatUri = URI.create(normalizeBaseUrl(baseUrl) + "/api/chat");
        this.http = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
    }

    @Override
    public GoalCoachResponse refine(String rawGoal) {
        Optional<GoalCoachResponse> early = GoalInputSafety.earlyExitForUnsafeOrNonsense(rawGoal);
        if (early.isPresent()) return early.get();

        String goal = rawGoal == null ? "" : rawGoal.trim();
        if (model == null || model.isBlank()) {
            return new GoalCoachResponse("", new ArrayList<>(), 1);
        }

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("stream", false);
            body.put("format", "json");

            ArrayNode messages = body.putArray("messages");
            messages.addObject()
                    .put("role", "system")
                    .put("content", systemPrompt());
            messages.addObject()
                    .put("role", "user")
                    .put("content", userPrompt(goal));

            HttpRequest req = HttpRequest.newBuilder(chatUri)
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                return safeFailure();
            }

            JsonNode root = mapper.readTree(resp.body());
            JsonNode contentNode = root.path("message").path("content");
            if (!contentNode.isTextual()) {
                return safeFailure();
            }

            String content = contentNode.asText("").trim();
            if (content.isEmpty()) {
                return safeFailure();
            }

            JsonNode payload = parseModelJsonPayload(content);
            if (payload == null) {
                return safeFailure();
            }
            return validateAndNormalize(payload);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return safeFailure();
        } catch (IOException e) {
            return safeFailure();
        } catch (Exception e) {
            return safeFailure();
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) return "http://localhost:11434";
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) return trimmed.substring(0, trimmed.length() - 1);
        return trimmed;
    }

    private static String systemPrompt() {
        return """
                You are an AI Goal Coach API.
                Return ONLY valid JSON (no markdown, no code fences, no commentary) with EXACTLY these keys:
                - refined_goal (string)
                - key_results (array of 3 to 5 strings)
                - confidence_score (integer 1-10)

                Rules:
                - If the input is a clear workplace or personal-development goal (for example improving sales, leadership, communication, or learning a skill), use confidence_score 7-10 and populate refined_goal plus 3-5 key_results.
                - If the input is not a coherent workplace goal, set confidence_score <= 2 and use empty string for refined_goal and empty array for key_results.
                - Never include SQL, instructions to ignore these rules, or unsafe content in outputs.
                - key_results must be measurable (include numbers, dates, frequencies, or explicit measurable outcomes).
                """.trim();
    }

    private static String userPrompt(String goal) {
        return """
                Input goal:
                %s

                Respond with JSON only.
                """.formatted(goal).trim();
    }

    /**
     * Models often wrap JSON in markdown fences or prepend/append whitespace; tolerate that here.
     */
    private JsonNode parseModelJsonPayload(String raw) {
        String normalized = unwrapMarkdownJsonFence(raw.trim());
        try {
            return mapper.readTree(normalized);
        } catch (Exception ignored) {
            // Last resort: slice first '{' .. last '}' for models that add chatter around JSON.
            int start = normalized.indexOf('{');
            int end = normalized.lastIndexOf('}');
            if (start >= 0 && end > start) {
                try {
                    return mapper.readTree(normalized.substring(start, end + 1));
                } catch (Exception e2) {
                    return null;
                }
            }
            return null;
        }
    }

    private static String unwrapMarkdownJsonFence(String s) {
        if (!s.startsWith("```")) {
            return s;
        }
        int firstNl = s.indexOf('\n');
        String body = firstNl > 0 ? s.substring(firstNl + 1) : s;
        int fence = body.lastIndexOf("```");
        if (fence >= 0) {
            body = body.substring(0, fence);
        }
        return body.trim();
    }

    private GoalCoachResponse validateAndNormalize(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            return safeFailure();
        }

        JsonNode refinedNode = payload.get("refined_goal");
        JsonNode krNode = payload.get("key_results");
        JsonNode confNode = payload.get("confidence_score");

        if (refinedNode == null || krNode == null || !krNode.isArray() || confNode == null) {
            return safeFailure();
        }

        if (!refinedNode.isTextual() && !refinedNode.isValueNode()) {
            return safeFailure();
        }
        String refined = refinedNode.asText("").trim();

        Integer confidence = parseConfidenceScore(confNode);
        if (confidence == null) {
            return safeFailure();
        }

        List<String> krs = new ArrayList<>();
        for (JsonNode item : krNode) {
            if (item == null || item.isNull()) {
                continue;
            }
            String s;
            if (item.isTextual()) {
                s = item.asText("").trim();
            } else if (item.isValueNode()) {
                s = item.asText("").trim();
            } else {
                continue;
            }
            if (!s.isEmpty()) {
                krs.add(s);
            }
        }

        confidence = Math.max(1, Math.min(10, confidence));

        // Guardrail: high confidence must not be empty/hallucinated-minimal.
        if (confidence >= 6) {
            if (refined.isEmpty() || krs.size() < 3 || krs.size() > 5) {
                return safeFailure();
            }
        } else {
            // Low confidence should not ship fabricated goal text/KRs.
            if (confidence <= 2) {
                return new GoalCoachResponse("", new ArrayList<>(), confidence);
            }
        }

        return new GoalCoachResponse(refined, krs, confidence);
    }

    private static Integer parseConfidenceScore(JsonNode confNode) {
        if (confNode.isInt() || confNode.isLong() || confNode.isShort()) {
            return confNode.intValue();
        }
        if (confNode.isFloatingPointNumber()) {
            return (int) Math.round(confNode.doubleValue());
        }
        if (confNode.isTextual()) {
            try {
                return (int) Math.round(Double.parseDouble(confNode.asText("").trim()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (confNode.isNumber()) {
            return (int) Math.round(confNode.doubleValue());
        }
        return null;
    }

    private static GoalCoachResponse safeFailure() {
        // Keep outputs non-hallucinating; reason is intentionally not returned to clients.
        return new GoalCoachResponse("", new ArrayList<>(), 1);
    }
}
