package br.distributed.system.chat.controller.server;

import br.distributed.system.chat.domain.dto.CreateNickRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/public/nick")
class NickController {
    @PostMapping
    public ResponseEntity<CreateNickRequestDTO> register(@RequestBody CreateNickRequestDTO req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(req);
    }
}
