package br.distributed.system.chat.controller.server;

import br.distributed.system.chat.domain.dto.ListMessagesResponseDTO;
import br.distributed.system.chat.domain.dto.MessageResponseDTO;
import br.distributed.system.chat.domain.dto.PostMessageRequestDTO;
import br.distributed.system.chat.service.MessageService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/public/groups/{groupId}/message")
class MessageController {
    private final MessageService service;

    public MessageController(MessageService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MessageResponseDTO> post(
            @PathVariable Long groupId,
            @RequestBody PostMessageRequestDTO req
    ) {
        var resp = service.postMessage(groupId, req);
        // 200 se foi deduplicação? Aqui simplificamos retornando 201 sempre que chegar até aqui com save/find.
        // Para diferenciar, seria necessário sinal do service; opcionalmente manter 200 ao detectar encontrado.
        return ResponseEntity.status(201).body(resp);
    }

    @GetMapping
    public ListMessagesResponseDTO list(
            @PathVariable Long groupId,
            @RequestParam(value = "since", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit
    ) {
        return service.listMessages(groupId, since, limit);
    }
}
