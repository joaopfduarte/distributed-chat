package br.distributed.system.chat.service;

import br.distributed.system.chat.repository.GroupRepository;
import br.distributed.system.chat.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Periodically checks if groups have active clients; if not, deletes messages for that group.
 */
@Component
@EnableScheduling
public class GroupJanitor {
    private static final Logger log = LoggerFactory.getLogger(GroupJanitor.class);

    private final GroupRepository groupRepository;
    private final MessageRepository messageRepository;
    private final GroupActivityTracker activityTracker;

    private static final Duration ACTIVITY_WINDOW = Duration.ofSeconds(10);

    public GroupJanitor(GroupRepository groupRepository,
                        MessageRepository messageRepository,
                        GroupActivityTracker activityTracker) {
        this.groupRepository = groupRepository;
        this.messageRepository = messageRepository;
        this.activityTracker = activityTracker;
    }

    @Scheduled(fixedDelay = 5000)
    @org.springframework.transaction.annotation.Transactional
    public void purgeInactiveGroupsMessages() {
        groupRepository.findAll().forEach(g -> {
            long gid = g.getId();
            var now = Instant.now();
            long ageSec = g.getCreatedAt() == null ? Long.MAX_VALUE : java.time.Duration.between(g.getCreatedAt(), now).getSeconds();

            boolean active10s = activityTracker.hasActiveClients(gid, ACTIVITY_WINDOW);
            if (!active10s && ageSec >= ACTIVITY_WINDOW.getSeconds()) {
                log.info("No active clients for group {} within {}s. Deleting messages...", gid, ACTIVITY_WINDOW.getSeconds());
                messageRepository.deleteByGroupId(gid);
            }
            boolean active60s = activityTracker.hasActiveClients(gid, Duration.ofSeconds(60));
            if (!active60s && ageSec >= 60) {
                log.info("No active clients for group {} within 60s. Deleting group...", gid);
                // ensure messages are gone, then delete group
                messageRepository.deleteByGroupId(gid);
                groupRepository.deleteById(gid);
                activityTracker.clearGroup(gid);
            }
        });
    }
}
