package br.distributed.system.chat.service.server;

import br.distributed.system.chat.domain.Groups;
import br.distributed.system.chat.domain.Message;
import br.distributed.system.chat.domain.dto.ListMessagesResponseDTO;
import br.distributed.system.chat.domain.dto.MessageResponseDTO;
import br.distributed.system.chat.domain.dto.PostMessageRequestDTO;
import br.distributed.system.chat.domain.mapper.MessageMapper;
import br.distributed.system.chat.repository.MessageRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;

    private final GroupService groupService;

    public MessageService(MessageRepository messageRepository, GroupService groupService) {
        this.messageRepository = messageRepository;
        this.groupService = groupService;
    }

    @Transactional
    public MessageResponseDTO postMessage(Long groupId, PostMessageRequestDTO postMessageRequestDTO) {
        if (postMessageRequestDTO == null || postMessageRequestDTO.idemKey() == null || postMessageRequestDTO.idemKey().isBlank())
            throw new IllegalArgumentException("idemKey required");
        if (postMessageRequestDTO.text() == null || postMessageRequestDTO.text().isBlank())
            throw new IllegalArgumentException("text required");
        if (postMessageRequestDTO.nickName() == null || postMessageRequestDTO.nickName().isBlank())
            throw new IllegalArgumentException("nickname required");

        log.info("Enviando mensagem para o grupo {}", groupId);
        groupService.findGroupById(groupId);

        return messageRepository.findByGroupIdAndIdemKey(groupId, postMessageRequestDTO.idemKey())
                .map(MessageMapper::toDto)
                .orElseGet(() -> {
                    Message m = new Message();
                    m.setGroupId(groupId);
                    m.setIdemKey(postMessageRequestDTO.idemKey());
                    m.setText(postMessageRequestDTO.text());
                    m.setNickName(postMessageRequestDTO.nickName());
                    m.setTimestampClient(postMessageRequestDTO.timestampClient());
                    Message saved = messageRepository.save(m);
                    return MessageMapper.toDto(saved);
                });
    }

    @Transactional(readOnly = true)
    public ListMessagesResponseDTO listMessages(Long groupId, Instant since, int limit) {
        log.info("Iniciando busca de mensagens | groupId={}, since={}, limit={}", groupId, since, limit);

        log.debug("Verificando existência do grupo com ID {}", groupId);
        Groups group = groupService.findGroupById(groupId);
        if (group == null) {
            log.error("Falha na busca de mensagens: grupo com ID {} não encontrado", groupId);
            throw new IllegalArgumentException("Grupo não encontrado");
        }
        log.info("Grupo encontrado: id={}, nome={}", group.getId(), group.getName()); // supondo que tenha getName()

        Instant cursor = since != null ? since : Instant.EPOCH;
        log.debug("Cursor calculado para busca: {}", cursor);

        int pageSize = Math.max(1, Math.min(limit > 0 ? limit : 10, 100));
        log.debug("PageSize definido: {}", pageSize);

        log.info("Consultando mensagens no repositório | groupId={}, cursor={}, pageSize={}", groupId, cursor, pageSize);
        List<Message> msgs = messageRepository
                .findByGroupIdAndTimestampServerGreaterThanOrderByTimestampServerAsc(
                        groupId, cursor, PageRequest.of(0, pageSize));

        log.info("Consulta concluída: {} mensagens encontradas", msgs.size());

        var items = msgs.stream().map(MessageMapper::toDto).toList();
        log.debug("Mensagens mapeadas para DTO: {} itens", items.size());

        String nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).timestampServer().toString();
        log.debug("Próximo cursor calculado: {}", nextCursor);

        log.info("Finalizando listagem de mensagens | groupId={}, totalRetornado={}, nextCursor={}",
                groupId, items.size(), nextCursor);

        return new ListMessagesResponseDTO(items, nextCursor);
    }


}
