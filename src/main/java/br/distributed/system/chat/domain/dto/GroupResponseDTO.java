package br.distributed.system.chat.domain.dto;

import java.time.Instant;

public record GroupResponseDTO(Long id, String name, Instant createdAt) {
}
