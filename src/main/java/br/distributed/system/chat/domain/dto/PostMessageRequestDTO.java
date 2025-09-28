package br.distributed.system.chat.domain.dto;

import java.time.Instant;

public record PostMessageRequestDTO(String idemKey,
                                    String text,
                                    Instant timestampClient,
                                    String nickName) {
}
