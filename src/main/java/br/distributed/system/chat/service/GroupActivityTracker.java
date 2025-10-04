package br.distributed.system.chat.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-group client activity based on message GET/POST requests.
 */
@Component
public class GroupActivityTracker {
    private final Map<Long, Map<String, Instant>> groupActivity = new ConcurrentHashMap<>();

    public void onRead(String clientId, long groupId) {
        if (clientId == null || clientId.isBlank()) return;
        groupActivity
                .computeIfAbsent(groupId, k -> new ConcurrentHashMap<>())
                .put(clientId, Instant.now());
    }

    public void onSend(String clientId, long groupId) {
        onRead(clientId, groupId);
    }

    public boolean hasActiveClients(long groupId, Duration window) {
        Map<String, Instant> clients = groupActivity.get(groupId);
        if (clients == null || clients.isEmpty()) return false;
        Instant now = Instant.now();
        return clients.values().stream().anyMatch(t -> Duration.between(t, now).compareTo(window) <= 0);
    }

    public void clearGroup(long groupId) {
        groupActivity.remove(groupId);
    }
}
