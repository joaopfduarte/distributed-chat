package br.distributed.system.chat.controller.server;

import br.distributed.system.chat.domain.dto.ListMessagesResponseDTO;
import br.distributed.system.chat.domain.dto.MessageResponseDTO;
import br.distributed.system.chat.domain.dto.PostMessageRequestDTO;
import br.distributed.system.chat.service.server.MessageService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/groups/{groupId}/messages")
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
        // Responder 200 somente após armazenar no SGBD (novo ou deduplicado).
        return ResponseEntity.ok(resp);
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
