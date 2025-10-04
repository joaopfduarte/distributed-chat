package br.distributed.system.chat.config;

import br.distributed.system.chat.service.server.ClientSessionRegistry;
import br.distributed.system.chat.service.GroupActivityTracker;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filter that enforces client session presence, tracks per-group activity and enforces idle timeout on send inactivity.
 */
@Component
@Order(1)
public class ClientSessionFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(ClientSessionFilter.class);

    private final ClientSessionRegistry registry;
    private final GroupActivityTracker activity;
    private final Duration idleThreshold;

    private final Map<String, Instant> lastSendLocal = new ConcurrentHashMap<>();

    public ClientSessionFilter(ClientSessionRegistry registry,
                               GroupActivityTracker activity,
                               @Value("${chat.idle-threshold-seconds:20}") long idleSeconds) {
        this.registry = registry;
        this.activity = activity;
        this.idleThreshold = Duration.ofSeconds(Math.max(1, idleSeconds));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        String method = req.getMethod();

        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String clientId = req.getHeader(ClientSessionRegistry.CLIENT_ID_HEADER);

        if (path.matches("^/groups/\\d+/messages(?:/)?(?:.*)?$")) {
            if (!registry.isValid(clientId)) {
                respondSessionInvalid(res);
                return;
            }
            long groupId = extractGroupId(path);
            if (method.equalsIgnoreCase("GET")) {
                // Track read activity for group cleanup logic
                activity.onRead(clientId, groupId);
                // Enforce idle timeout on reads (20s sem enviar)
                Instant lastSend = lastSendLocal.get(clientId);
                if (lastSend != null && Duration.between(lastSend, Instant.now()).compareTo(idleThreshold) > 0) {
                    respondSessionInvalid(res);
                    return;
                }
            } else if (method.equalsIgnoreCase("POST")) {
                registry.touchOnSend(clientId);
                activity.onSend(clientId, groupId);
                lastSendLocal.put(clientId, Instant.now());
            }
        }

        chain.doFilter(request, response);
    }

    private void respondSessionInvalid(HttpServletResponse res) throws IOException {
        res.setStatus(440);
        res.getWriter().write("Sessão inválida ou expirada. Registre um novo nick em /nick para continuar.");
    }

    private boolean isPublicPath(String path) {
        // Groups listing/creation are public to allow UI bootstrapping
        return path.startsWith("/nick")
                || path.equals("/groups")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/h2-console")
                || path.equals("/");
    }

    private long extractGroupId(String path) {
        // path like /groups/{id}/messages...
        try {
            String[] parts = path.split("/");
            return Long.parseLong(parts[2]);
        } catch (Exception e) {
            return -1L;
        }
    }
}
