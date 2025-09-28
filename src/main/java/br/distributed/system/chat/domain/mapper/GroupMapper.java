package br.distributed.system.chat.domain.mapper;

import br.distributed.system.chat.domain.Groups;
import br.distributed.system.chat.domain.Message;
import br.distributed.system.chat.domain.dto.GroupResponseDTO;
import br.distributed.system.chat.domain.dto.MessageResponseDTO;
import org.mapstruct.Mapper;

@Mapper
public interface GroupMapper {
    public static GroupResponseDTO toDto(Groups g) {
        return new GroupResponseDTO(g.getId(), g.getName(), g.getCreatedAt());
    }


}
