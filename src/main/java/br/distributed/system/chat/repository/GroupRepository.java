package br.distributed.system.chat.repository;

import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.jpa.repository.JpaRepository;

interface GroupRepository extends JpaRepository<Group, Long> {
}
