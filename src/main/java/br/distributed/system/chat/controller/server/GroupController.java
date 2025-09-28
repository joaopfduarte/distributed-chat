package br.distributed.system.chat.controller.server;

import br.distributed.system.chat.domain.dto.CreateGroupRequestDTO;
import br.distributed.system.chat.domain.dto.GroupResponseDTO;
import br.distributed.system.chat.domain.mapper.GroupMapper;
import br.distributed.system.chat.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/public/group")
class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupResponseDTO> create(@RequestBody CreateGroupRequestDTO req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        var created = groupService.createGroup(req.name());
        var body = GroupMapper.toDto(created);
        return ResponseEntity.created(URI.create("/groups/" + created.getId())).body(body);
    }

    @GetMapping
    public java.util.List<GroupResponseDTO> list() {
        return groupService.listGroups().stream().map(GroupMapper::toDto).toList();
    }
}
