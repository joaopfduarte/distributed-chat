package br.distributed.system.chat.service.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Minimal reference client for the chat server.
 *
 * Responsibilities:
 * - Register nickname (POST /nick) and manage X-Client-Id header
 * - Create/list groups
 * - Post messages with retry + exponential backoff with jitter
 * - Read messages with cursor (since) and limit
 * - Handle server-driven eviction (HTTP 440) by re-registering automatically
 *
 * This class is dependency-light and uses Java 11 HttpClient and Jackson (provided by Spring deps).
 */
public class ChatClient {
    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper json;

    private String clientId;
    private String nick;

    public ChatClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.json = new ObjectMapper();
    }

    public Optional<String> getClientId() { return Optional.ofNullable(clientId); }
    public String getNick() { return nick; }

    // =============== Registration ===============
    public void registerNick(String nick) throws IOException, InterruptedException {
        this.nick = nick;
        var body = json.writeValueAsString(Map.of("name", nick));
        var req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/nick"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("Failed to register nick: status=" + resp.statusCode() + ", body=" + resp.body());
        }
        String cid = resp.headers().firstValue("X-Client-Id").orElse(null);
        if (cid == null || cid.isBlank()) {
            throw new IOException("Server did not return X-Client-Id header");
        }
        this.clientId = cid;
    }

    // =============== Groups ===============
    public long createGroup(String name) throws IOException, InterruptedException {
        var body = json.writeValueAsString(Map.of("name", name));
        var req = baseRequest("/groups")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        ensureOkOrThrow(resp);
        Map<String, Object> dto = json.readValue(resp.body(), new TypeReference<>(){});
        Number id = (Number) dto.get("id");
        return id.longValue();
    }

    public List<Map<String, Object>> listGroups() throws IOException, InterruptedException {
        var req = baseRequest("/groups").GET().build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        ensureOkOrThrow(resp);
        return json.readValue(resp.body(), new TypeReference<>(){});
    }

    // =============== Messages ===============
    public Map<String, Object> postMessage(long groupId, String text) throws IOException, InterruptedException {
        return postMessage(groupId, text, null, null, 5, 100, 2_000);
    }

    public Map<String, Object> postMessage(
            long groupId,
            String text,
            String idemKey,
            Instant timestampClient,
            int maxAttempts,
            long baseBackoffMs,
            long maxBackoffMs
    ) throws IOException, InterruptedException {
        if (idemKey == null || idemKey.isBlank()) {
            idemKey = UUID.randomUUID().toString();
        }
        if (timestampClient == null) {
            timestampClient = Instant.now();
        }
        var payload = Map.of(
                "idemKey", idemKey,
                "text", text,
                "timestampClient", timestampClient.toString(),
                "nickName", nick
        );
        String path = "/groups/" + groupId + "/messages";

        int attempt = 0;
        while (true) {
            attempt++;
            var req = baseRequest(path)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)))
                    .build();
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 440) {
                reRegister();
            } else if (resp.statusCode() / 100 == 2) {
                return json.readValue(resp.body(), new TypeReference<>(){});
            } else if (shouldRetry(resp.statusCode()) && attempt < maxAttempts) {
                sleepBackoff(attempt, baseBackoffMs, maxBackoffMs);
            } else {
                throw new IOException("postMessage failed: status=" + resp.statusCode() + ", body=" + resp.body());
            }
            if (attempt >= maxAttempts) {
                throw new IOException("postMessage failed after attempts=" + attempt);
            }
        }
    }

    public Map<String, Object> listMessages(long groupId, Instant since, int limit) throws IOException, InterruptedException {
        String qs = "";
        if (since != null) {
            qs += (qs.isEmpty()?"?":"&") + "since=" + urlEnc(since.toString());
        }
        if (limit > 0) {
            qs += (qs.isEmpty()?"?":"&") + "limit=" + limit;
        }
        var reqBuilder = baseRequest("/groups/" + groupId + "/messages" + qs)
                .GET();
        var req = reqBuilder.build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 440) {
            reRegister();
            // rebuild request to include new X-Client-Id and retry once
            req = baseRequest("/groups/" + groupId + "/messages" + qs).GET().build();
            resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        }
        ensureOkOrThrow(resp);
        return json.readValue(resp.body(), new TypeReference<>(){});
    }

    // =============== Helpers ===============
    private HttpRequest.Builder baseRequest(String path) {
        var b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(5));
        if (clientId != null && !clientId.isBlank()) {
            b.header("X-Client-Id", clientId);
        }
        return b;
    }

    private void reRegister() throws IOException, InterruptedException {
        if (nick == null || nick.isBlank()) throw new IOException("Cannot re-register: nick not set");
        registerNick(nick);
    }

    private static boolean shouldRetry(int status) {
        return status == 408 || status == 429 || (status >= 500 && status < 600);
    }

    private static String urlEnc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static void sleepBackoff(int attempt, long baseMs, long maxMs) {
        long delay = Backoff.computeDelayMs(attempt, baseMs, maxMs);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ignored) {}
    }

    private void ensureOkOrThrow(HttpResponse<String> resp) throws IOException {
        int sc = resp.statusCode();
        if (sc / 100 != 2) {
            throw new IOException("HTTP call failed: status=" + sc + ", body=" + resp.body());
        }
    }
}