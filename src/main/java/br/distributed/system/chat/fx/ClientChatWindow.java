package br.distributed.system.chat.fx;

import br.distributed.system.chat.service.client.ChatClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A chat window bound to a specific group and nick.
 */
public class ClientChatWindow extends Stage {
    private final String baseUrl;
    private final long groupId;
    private final String nick;

    private final ChatClient client;
    private final TextArea txtMessages = new TextArea();
    private final TextField txtInput = new TextField();
    private final Button btnSend = new Button("Enviar");

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "chat-poller");
        t.setDaemon(true);
        return t;
    });

    private volatile Instant sinceCursor = null; // server timestamp cursor

    public ClientChatWindow(String baseUrl, long groupId, String nick) {
        this.baseUrl = Objects.requireNonNull(baseUrl);
        this.groupId = groupId;
        this.nick = Objects.requireNonNull(nick);
        this.client = new ChatClient(baseUrl);

        setTitle("Cliente: " + nick + " | Grupo " + groupId);
        var root = new BorderPane();
        root.setPadding(new Insets(10));

        txtMessages.setEditable(false);
        txtMessages.setWrapText(true);
        root.setCenter(txtMessages);

        var bottom = new HBox(8, txtInput, btnSend);
        root.setBottom(bottom);
        txtInput.setPromptText("Digite sua mensagem e pressione Enviar...");

        btnSend.setOnAction(e -> doSend());
        txtInput.setOnAction(e -> doSend());

        setScene(new Scene(root, 700, 400));

        setOnCloseRequest(e -> scheduler.shutdownNow());

        // Register nick then start polling
        new Thread(() -> {
            try {
                client.registerNick(nick);
                startPolling();
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Falha ao registrar nick: " + ex.getMessage()));
            }
        }, "register-nick").start();
    }

    private void doSend() {
        String text = txtInput.getText().trim();
        if (text.isBlank()) return;
        txtInput.clear();
        new Thread(() -> {
            try {
                client.postMessage(groupId, text);
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Falha ao enviar: " + ex.getMessage()));
            }
        }, "send-thread").start();
    }

    private void startPolling() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                Map<String, Object> resp = client.listMessages(groupId, sinceCursor, 50);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) resp.get("messages");
                String nextCursor = (String) resp.get("nextCursor");
                if (items != null && !items.isEmpty()) {
                    Platform.runLater(() -> {
                        for (Map<String, Object> m : items) {
                            String n = String.valueOf(m.get("nickName"));
                            String t = String.valueOf(m.get("text"));
                            String ts = String.valueOf(m.get("timestampServer"));
                            txtMessages.appendText(String.format("[%s] %s: %s%n", ts, n, t));
                        }
                    });
                }
                if (nextCursor != null && !nextCursor.isBlank()) {
                    sinceCursor = Instant.parse(nextCursor);
                }
            } catch (Exception ex) {
                Platform.runLater(() -> showInfo("Poll falhou: " + ex.getMessage()));
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).show();
    }
    private void showInfo(String msg) {
        // less intrusive: status bar could be better; for now, ignore frequent poll errors
    }
}
