package br.distributed.system.chat.repository;

import br.distributed.system.chat.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

interface MessageRepository extends JpaRepository<Message, Long> {
}
