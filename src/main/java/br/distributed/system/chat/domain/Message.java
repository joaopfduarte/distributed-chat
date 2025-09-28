package br.distributed.system.chat.domain;

import jakarta.persistence.*;
import org.springframework.boot.autoconfigure.web.WebProperties;

import java.time.Instant;

@Entity
@Table(name = "message", indexes = {
        @Index(name = "idx_msg_group_ts", columnList = "group_id,timestamp_server")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_msg_group_idem", columnNames = {"group_id", "idem_key"})
})
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(nullable = false, length = 64)
    private String nickName;

    @Column(nullable = false, length = 4000)
    private String text;

    @Column(name = "idem_key", nullable = false, length = 128)
    private String idemKey;

    @Column(name = "timestamp_client")
    private Instant timestampClient;

    @Column(name = "timestamp_server", nullable = false, updatable = false)
    private Instant timestampServer;

    @PrePersist
    public void prePersist() {
        if (timestampServer == null) timestampServer = Instant.now();
    }

    public Long getId() {return id;}

    public void setId(Long id) {this.id = id;}

    public Long getGroupId() {return groupId;}

    public void setGroupId(Long groupId) {this.groupId = groupId;}

    public String getNickName() {return nickName;}

    public void setNickName(String nickName) {this.nickName = nickName;}

    public String getText() {return text;}

    public void setText(String text) {this.text = text;}

    public String getIdemKey() {return idemKey;}

    public void setIdemKey(String idemKey) {this.idemKey = idemKey;}

    public Instant getTimestampClient() {return timestampClient;}

    public void setTimestampClient(Instant timestampClient) {this.timestampClient = timestampClient;}

    public Instant getTimestampServer() {return timestampServer;}

    public void setTimestampServer(Instant timestampServer) {this.timestampServer = timestampServer;}
}
