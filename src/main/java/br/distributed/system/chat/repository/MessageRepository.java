package br.distributed.system.chat.repository;

import br.distributed.system.chat.domain.Message;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    Optional<Message> findByGroupIdAndIdemKey(Long groupId, String idemKey);

    List<Message> findByGroupIdAndTimestampServerGreaterThanOrderByTimestampServerAsc(Long groupId, Instant timestampServer, Pageable pageable);

}
