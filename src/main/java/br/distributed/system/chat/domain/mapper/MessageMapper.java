package br.distributed.system.chat.domain.mapper;

import br.distributed.system.chat.domain.Message;
import br.distributed.system.chat.domain.dto.MessageResponseDTO;
import org.mapstruct.Mapper;

@Mapper
public interface MessageMapper {
    public static MessageResponseDTO toDto(Message m) {
        return new MessageResponseDTO(
                m.getId(),
                m.getGroupId(),
                m.getNickName(),
                m.getText(),
                m.getTimestampClient(),
                m.getTimestampServer(),
                m.getIdemKey()
        );
    }
}
