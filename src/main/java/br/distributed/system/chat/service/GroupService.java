package br.distributed.system.chat.service;

import br.distributed.system.chat.repository.GroupRepository;
import br.distributed.system.chat.domain.Groups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GroupService {

    private static final Logger log = LoggerFactory.getLogger(GroupService.class);

    private final GroupRepository groupRepository;

    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Transactional
    public Groups createGroup(String name) {
        log.info("Inicializando criação de grupo");
        Optional<Groups> existingGroup = groupRepository.findByName(name);
        if (existingGroup.isPresent()) {
            log.error("Grupo já criado. Por favor tente criar outro grupo.");
            throw new IllegalStateException("Grupo ja existente");
        }
        Groups newGroup = new Groups();
        newGroup.setName(name);

        groupRepository.save(newGroup);
        log.info("Group {} criado com sucesso", name);
        return newGroup;
    }

    @Transactional(readOnly = true)
    public List<Groups> listGroups() {
        log.info("Busca de todos os grupos");
        return groupRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Groups findGroupById(Long id) {
    log.info("Busca do grupo pelo ID {}", id);
        Optional<Groups> group = groupRepository.findById(id);
        if (group.isEmpty()) {
            throw new IllegalStateException("Grupo não encontrado");
        }

        return group.get();
    }
}
