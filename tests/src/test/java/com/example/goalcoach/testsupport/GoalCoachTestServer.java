package com.example.goalcoach.testsupport;

import com.example.goalcoach.model.GoalCoachResponse;
import com.example.goalcoach.engine.GoalCoachEngine;
import com.example.goalcoach.engine.GoalCoachEngines;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GoalCoachTestServer {
    private final ObjectMapper mapper = new ObjectMapper();
    private final GoalCoachEngine engine = GoalCoachEngines.create(mapper);
    private HttpServer server;

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/goal-coach", new ApiHandler());
        server.createContext("/", new HomeHandler());
        server.createContext("/submit", new SubmitHandler());
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private abstract class BaseHandler implements HttpHandler {
        @Override
        public final void handle(HttpExchange exchange) throws IOException {
            String requestId = headerOr(exchange.getRequestHeaders(), "X-Request-Id");
            if (requestId == null || requestId.trim().isEmpty()) requestId = UUID.randomUUID().toString();
            exchange.getResponseHeaders().set("X-Request-Id", requestId);
            doHandle(exchange);
        }

        protected abstract void doHandle(HttpExchange exchange) throws IOException;
    }

    private class ApiHandler extends BaseHandler {
        @Override
        protected void doHandle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "application/json", "{\"error\":\"method_not_allowed\"}");
                return;
            }
            String body = readAll(exchange.getRequestBody());
            String goal = "";
            try {
                JsonNode n = mapper.readTree(body);
                JsonNode g = n.get("goal");
                goal = g == null || g.isNull() ? "" : g.asText("");
            } catch (Exception e) {
                // Bad JSON -> treat as invalid input; return low confidence without hallucination.
                goal = "";
            }

            GoalCoachResponse resp = engine.refine(goal);
            String json = mapper.writeValueAsString(resp);
            send(exchange, 200, "application/json", json);
        }
    }

    private class HomeHandler extends BaseHandler {
        @Override
        protected void doHandle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
                return;
            }
            String html =
                    "<!doctype html>\n" +
                    "<html lang=\"en\">\n" +
                    "  <head>\n" +
                    "    <meta charset=\"utf-8\"/>\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>\n" +
                    "    <title>AI Goal Coach</title>\n" +
                    "    <style>\n" +
                    "      body { font-family: -apple-system, system-ui, Segoe UI, Roboto, Helvetica, Arial, sans-serif; max-width: 860px; margin: 40px auto; padding: 0 16px; }\n" +
                    "      .card { border: 1px solid #e5e7eb; border-radius: 12px; padding: 16px; }\n" +
                    "      label { display:block; font-weight: 600; margin-bottom: 8px; }\n" +
                    "      textarea { width: 100%; height: 120px; padding: 10px; border-radius: 10px; border: 1px solid #d1d5db; }\n" +
                    "      button { margin-top: 12px; padding: 10px 14px; border-radius: 10px; border: 1px solid #111827; background: #111827; color: white; font-weight: 600; cursor: pointer; }\n" +
                    "      .hint { color: #6b7280; font-size: 14px; margin-top: 6px; }\n" +
                    "    </style>\n" +
                    "  </head>\n" +
                    "  <body>\n" +
                    "    <h1>AI Goal Coach</h1>\n" +
                    "    <div class=\"card\">\n" +
                    "      <form method=\"post\" action=\"/submit\">\n" +
                    "        <label for=\"goal\">Your goal</label>\n" +
                    "        <textarea id=\"goal\" name=\"goal\" placeholder=\"e.g., I want to improve in sales\"></textarea>\n" +
                    "        <div class=\"hint\">Backend is configurable (stub by default; optional Ollama).</div>\n" +
                    "        <button id=\"submit\" type=\"submit\">Refine</button>\n" +
                    "      </form>\n" +
                    "    </div>\n" +
                    "  </body>\n" +
                    "</html>\n";
            send(exchange, 200, "text/html; charset=utf-8", html);
        }
    }

    private class SubmitHandler extends BaseHandler {
        @Override
        protected void doHandle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
                return;
            }
            String body = readAll(exchange.getRequestBody());
            Map<String, String> form = parseForm(body);
            String goal = form.get("goal");
            GoalCoachResponse r = engine.refine(goal);

            StringBuilder kr = new StringBuilder();
            if (r.getKey_results() != null) {
                for (String s : r.getKey_results()) {
                    kr.append("<li class=\"kr\">").append(escape(s)).append("</li>\n");
                }
            }

            String html =
                    "<!doctype html>\n" +
                    "<html lang=\"en\">\n" +
                    "  <head>\n" +
                    "    <meta charset=\"utf-8\"/>\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>\n" +
                    "    <title>AI Goal Coach Result</title>\n" +
                    "  </head>\n" +
                    "  <body>\n" +
                    "    <h1>Result</h1>\n" +
                    "    <div>\n" +
                    "      <div>Confidence</div>\n" +
                    "      <div id=\"confidence\">" + r.getConfidence_score() + "</div>\n" +
                    "      <div>Refined goal</div>\n" +
                    "      <div id=\"refinedGoal\">" + escape(r.getRefined_goal()) + "</div>\n" +
                    "      <div>Key results</div>\n" +
                    "      <ul id=\"keyResults\">" + kr.toString() + "</ul>\n" +
                    "      <a id=\"back\" href=\"/\">Back</a>\n" +
                    "    </div>\n" +
                    "  </body>\n" +
                    "</html>\n";
            send(exchange, 200, "text/html; charset=utf-8", html);
        }
    }

    private static String headerOr(Headers headers, String name) {
        if (headers == null) return null;
        if (!headers.containsKey(name)) return null;
        if (headers.get(name) == null || headers.get(name).isEmpty()) return null;
        return headers.get(name).get(0);
    }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = is.read(buf)) != -1) bos.write(buf, 0, r);
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        try {
            os.write(bytes);
        } finally {
            os.close();
        }
    }

    private static Map<String, String> parseForm(String body) throws IOException {
        Map<String, String> out = new HashMap<String, String>();
        if (body == null || body.trim().isEmpty()) return out;
        String[] parts = body.split("&");
        for (String p : parts) {
            int idx = p.indexOf('=');
            if (idx < 0) continue;
            String k = URLDecoder.decode(p.substring(0, idx), "UTF-8");
            String v = URLDecoder.decode(p.substring(idx + 1), "UTF-8");
            out.put(k, v);
        }
        return out;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

