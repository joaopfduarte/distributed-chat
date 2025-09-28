package br.distributed.system.chat.domain.dto;

import java.util.List;

public record ListMessagesResponseDTO(List<MessageResponseDTO> messages, String nextCursor) {

}
