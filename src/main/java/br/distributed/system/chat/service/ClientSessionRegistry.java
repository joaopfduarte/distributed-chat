package br.distributed.system.chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry to track active clients (logical connections) and enforce a max-connections policy.
 *
 * Semantics:
 * - A client registers via POST /nick and receives a clientId (UUID) in the X-Client-Id response header.
 * - Clients must send X-Client-Id in subsequent requests.
 * - We only update lastSendAt when the client posts a message; reads don't count for activity window.
 * - If a 6th client tries to register, we evict one: first prefer any idle (> idleThreshold) by lastSendAt,
 *   otherwise evict the oldest by connectedAt.
 * - Evicted clients become invalid and further requests will be rejected until they re-register.
 */
@Component
public class ClientSessionRegistry {
    private static final Logger log = LoggerFactory.getLogger(ClientSessionRegistry.class);

    public static final String CLIENT_ID_HEADER = "X-Client-Id";

    private final int maxClients;
    private final Duration idleThreshold;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public ClientSessionRegistry(
            @Value("${chat.max-clients:5}") int maxClients,
            @Value("${chat.idle-threshold-seconds:15}") long idleSeconds
    ) {
        this.maxClients = Math.max(1, maxClients);
        this.idleThreshold = Duration.ofSeconds(Math.max(1, idleSeconds));
    }

    public static final class Registration {
        public final String clientId;
        public final String evictedClientId;
        public final String evictedNick;

        Registration(String clientId, String evictedClientId, String evictedNick) {
            this.clientId = clientId;
            this.evictedClientId = evictedClientId;
            this.evictedNick = evictedNick;
        }
    }

    private static final class Session {
        final String clientId;
        final String nick;
        final Instant connectedAt;
        volatile Instant lastSendAt;
        volatile boolean evicted;
        Session(String clientId, String nick, Instant now) {
            this.clientId = clientId;
            this.nick = nick;
            this.connectedAt = now;
            this.lastSendAt = now;
            this.evicted = false;
        }
    }

    public synchronized Registration register(String nick) {
        Objects.requireNonNull(nick, "nick");
        var now = Instant.now();
        if (sessions.size() >= maxClients) {
            Optional<Session> idleCandidate = sessions.values().stream()
                    .filter(s -> !s.evicted)
                    .filter(s -> Duration.between(s.lastSendAt, now).compareTo(idleThreshold) > 0)
                    .findAny();

            Session toEvict = idleCandidate.orElseGet(() -> sessions.values().stream()
                    .filter(s -> !s.evicted)
                    .min(Comparator.comparing(s -> s.connectedAt))
                    .orElse(null));

            if (toEvict != null) {
                toEvict.evicted = true;
                sessions.remove(toEvict.clientId);
                log.warn("Evicting client due to capacity: clientId={}, nick={}, connectedAt={}, lastSendAt={}",
                        toEvict.clientId, toEvict.nick, toEvict.connectedAt, toEvict.lastSendAt);
                String newId = UUID.randomUUID().toString();
                sessions.put(newId, new Session(newId, nick, now));
                return new Registration(newId, toEvict.clientId, toEvict.nick);
            } else {
                String newId = UUID.randomUUID().toString();
                sessions.put(newId, new Session(newId, nick, now));
                return new Registration(newId, null, null);
            }
        } else {
            String newId = UUID.randomUUID().toString();
            sessions.put(newId, new Session(newId, nick, now));
            log.info("Registered new client: clientId={}, nick={}", newId, nick);
            return new Registration(newId, null, null);
        }
    }

    public boolean isValid(String clientId) {
        if (clientId == null || clientId.isBlank()) return false;
        Session s = sessions.get(clientId);
        return s != null && !s.evicted;
    }

    public void touchOnSend(String clientId) {
        if (clientId == null) return;
        Session s = sessions.get(clientId);
        if (s != null && !s.evicted) {
            s.lastSendAt = Instant.now();
        }
    }

    public Optional<String> nickOf(String clientId) {
        Session s = sessions.get(clientId);
        return s == null ? Optional.empty() : Optional.ofNullable(s.nick);
    }
}
