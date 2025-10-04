package br.distributed.system.chat.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        long start = System.nanoTime();
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String rid = req.getHeader("X-Request-Id");
        if (rid == null || rid.isBlank()) rid = UUID.randomUUID().toString();
        MDC.put("rid", rid);
        try {
            chain.doFilter(request, response);
        } finally {
            long durMs = (System.nanoTime() - start) / 1_000_000;
            org.slf4j.LoggerFactory.getLogger(RequestLoggingFilter.class)
                    .info("rid={} method={} path={} status={} latency_ms={}",
                            rid, req.getMethod(), req.getRequestURI(), res.getStatus(), durMs);
            MDC.remove("rid");
        }
    }
}
