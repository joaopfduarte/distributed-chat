package br.distributed.system.chat.service.client;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/*
Classe de teste por terminal com apenas 1 cliente, ilustração
 */
public class ChatClientDemo {
    public static void main(String[] args) throws Exception {
        String baseUrl = System.getProperty("chat.baseUrl", "http://localhost:8080");
        String nick = System.getProperty("chat.nick", "alice");

        ChatClient client = new ChatClient(baseUrl);
        client.registerNick(nick);
        System.out.println("Registered nick=\"" + nick + "\" clientId=" + client.getClientId().orElse("N/A"));

        long groupId;
        var groups = client.listGroups();
        var general = groups.stream().filter(g -> "geral".equalsIgnoreCase((String) g.get("name"))).findFirst();
        if (general.isPresent()) {
            groupId = ((Number) general.get().get("id")).longValue();
            System.out.println("Using existing group 'geral' id=" + groupId);
        } else {
            groupId = client.createGroup("geral");
            System.out.println("Created group 'geral' id=" + groupId);
        }

        // Post a couple of messages
        client.postMessage(groupId, "Olá, mundo!");
        client.postMessage(groupId, "Mensagem 2 - teste de backoff e idempotência");

        // Read last messages
        Map<String, Object> page = client.listMessages(groupId, null, 10);
        System.out.println("Messages page: " + page);
        String nextCursor = (String) page.get("nextCursor");

        // Post another and read since cursor
        client.postMessage(groupId, "Mensagem 3 depois do cursor " + Instant.now());
        Map<String, Object> page2 = client.listMessages(groupId, nextCursor == null ? null : Instant.parse(nextCursor), 10);
        System.out.println("Messages page2: " + page2);

        System.out.println("Demo finished.");
    }
}
