package br.distributed.system.chat.domain.dto;

import java.time.Instant;

public record MessageResponseDTO(Long id,
                                 Long groupId,
                                 String nickName,
                                 String text,
                                 Instant timestampServer,
                                 Instant timestampClient,
                                 String idemKey) {
}
