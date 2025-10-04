package br.distributed.system.chat.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record PostMessageRequestDTO(String idemKey,
                                    String text,
                                    Instant timestampClient,
                                    String nickName) {
    public PostMessageRequestDTO {
        if (idemKey == null || idemKey.isBlank()) {
            idemKey = UUID.randomUUID().toString();
        }
        if (timestampClient == null) {
            timestampClient = Instant.now();
        }
    }
}
