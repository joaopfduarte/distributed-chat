package br.distributed.system.chat.fx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Chat Distribuído");
        stage.setScene(new Scene(new Label("Olá mundo"), 400, 200));
        stage.show();
    }
}
