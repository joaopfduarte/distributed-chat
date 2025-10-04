package br.distributed.system.chat.config;

import br.distributed.system.chat.service.ClientSessionRegistry;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter that enforces client session presence and updates activity on send operations.
 */
@Component
@Order(1)
public class ClientSessionFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(ClientSessionFilter.class);

    private final ClientSessionRegistry registry;

    public ClientSessionFilter(ClientSessionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        String method = req.getMethod();

        // Skip checks for registration and non-chat endpoints
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String clientId = req.getHeader(ClientSessionRegistry.CLIENT_ID_HEADER);

        // Enforce session ONLY for message endpoints: /groups/{id}/messages
        if (path.matches("^/groups/\\d+/messages(?:/)?(?:.*)?$")) {
            if (!registry.isValid(clientId)) {
                // 440 Login Time-out (non-standard) or 403; use 440 to signal session eviction/timeout.
                res.setStatus(440);
                res.getWriter().write("Sessão inválida ou expirada. Registre um novo nick em /nick para continuar.");
                return;
            }
            // Update lastSend only for POST /groups/{id}/messages
            if (method.equalsIgnoreCase("POST")) {
                registry.touchOnSend(clientId);
            }
        }

        chain.doFilter(request, response);
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
}
