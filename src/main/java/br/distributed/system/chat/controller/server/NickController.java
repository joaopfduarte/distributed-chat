package br.distributed.system.chat.controller.server;

import br.distributed.system.chat.domain.dto.CreateNickRequestDTO;
import br.distributed.system.chat.service.ClientSessionRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/nick")
class NickController {

    private final ClientSessionRegistry sessionRegistry;

    public NickController(ClientSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @PostMapping
    public ResponseEntity<CreateNickRequestDTO> register(@RequestBody CreateNickRequestDTO req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        var reg = sessionRegistry.register(req.name());
        return ResponseEntity.ok()
                .header(ClientSessionRegistry.CLIENT_ID_HEADER, reg.clientId)
                .body(req);
    }
}
