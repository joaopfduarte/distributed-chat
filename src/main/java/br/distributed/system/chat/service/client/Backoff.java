package br.distributed.system.chat.service.client;

import java.util.concurrent.ThreadLocalRandom;

public final class Backoff {
    private Backoff() {}

    public static long computeDelayMs(int attempt, long baseMs, long maxMs) {
        long exp = Math.min(maxMs, baseMs * (1L << Math.min(attempt, 10)));
        long jitter = ThreadLocalRandom.current().nextLong(0, baseMs);
        return Math.min(maxMs, exp + jitter);
    }
}