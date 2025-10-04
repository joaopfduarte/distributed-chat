package br.distributed.system.chat.fx;

import br.distributed.system.chat.service.client.ChatClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Main JavaFX view to manage server URL, groups and spawn client windows.
 */
public class MainView {
    private final BorderPane root = new BorderPane();

    private final TextField txtServer = new TextField("http://localhost:8080");
    private final TextField txtGroupName = new TextField();
    private final Button btnCreateGroup = new Button("Criar grupo");
    private final Button btnRefreshGroups = new Button("Atualizar");
    private final ListView<String> lstGroups = new ListView<>();

    private final TextField txtNick = new TextField();
    private final TextField txtGroupId = new TextField();
    private final Button btnOpenClient = new Button("Abrir cliente");

    public MainView() {
        root.setPadding(new Insets(10));

        // Top: server URL
        var top = new HBox(8, new Label("Servidor:"), txtServer);
        txtServer.setPrefColumnCount(40);
        root.setTop(top);

        // Center: groups
        var groupsBox = new VBox(8);
        groupsBox.setPadding(new Insets(10, 0, 10, 0));
        var gpControls = new HBox(8, new Label("Nome do grupo:"), txtGroupName, btnCreateGroup, btnRefreshGroups);
        groupsBox.getChildren().addAll(new Label("Grupos"), gpControls, lstGroups);
        root.setCenter(groupsBox);

        // Bottom: open client
        var grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8);
        grid.add(new Label("Nick:"), 0, 0);
        grid.add(txtNick, 1, 0);
        grid.add(new Label("GroupId:"), 0, 1);
        grid.add(txtGroupId, 1, 1);
        grid.add(btnOpenClient, 1, 2);
        root.setBottom(grid);

        btnCreateGroup.setOnAction(e -> createGroup());
        btnRefreshGroups.setOnAction(e -> refreshGroups());
        btnOpenClient.setOnAction(e -> openClientWindow());

        // Try initial load
        Platform.runLater(this::refreshGroups);
    }

    public Parent getView() {
        return root;
    }

    private void createGroup() {
        String baseUrl = txtServer.getText().trim();
        String name = txtGroupName.getText().trim();
        if (name.isBlank()) { alert("Informe um nome de grupo."); return; }
        var client = new ChatClient(baseUrl);
        new Thread(() -> {
            try {
                long id = client.createGroup(name);
                Platform.runLater(() -> {
                    alert("Grupo criado: id=" + id);
                    txtGroupName.clear();
                    refreshGroups();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> alertError("Falha ao criar grupo: " + ex.getMessage()));
            }
        }).start();
    }

    private void refreshGroups() {
        String baseUrl = txtServer.getText().trim();
        var client = new ChatClient(baseUrl);
        new Thread(() -> {
            try {
                List<Map<String, Object>> groups = client.listGroups();
                Platform.runLater(() -> {
                    lstGroups.getItems().clear();
                    for (var g : groups) {
                        Object id = g.get("id");
                        Object name = g.get("name");
                        lstGroups.getItems().add(id + " - " + name);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> alertError("Falha ao listar grupos: " + ex.getMessage()));
            }
        }).start();
    }

    private void openClientWindow() {
        String baseUrl = txtServer.getText().trim();
        String nick = txtNick.getText().trim();
        String gidStr = txtGroupId.getText().trim();
        if (nick.isBlank()) { alert("Informe o nick."); return; }
        if (gidStr.isBlank()) { alert("Informe o GroupId."); return; }
        long gid;
        try { gid = Long.parseLong(gidStr); } catch (NumberFormatException ex) { alert("GroupId inválido."); return; }
        var win = new ClientChatWindow(baseUrl, gid, nick);
        win.show();
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
    private void alertError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
