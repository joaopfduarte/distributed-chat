package br.distributed.system.chat.fx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Chat Distribuído");
        MainView main = new MainView();
        stage.setScene(new Scene(main.getView(), 800, 600));
        stage.show();
    }
}
