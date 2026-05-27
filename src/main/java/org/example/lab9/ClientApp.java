package org.example.lab9;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        ChatModel model = new ChatModel();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("chat-view.fxml"));

        loader.setControllerFactory(param -> {
            if (param == ChatController.class) {
                return new ChatController(model);
            }
            try {
                return param.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        var root = loader.load();
        primaryStage.setTitle("Тайный друн");
        primaryStage.setScene(new Scene((Parent) root));
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> model.disconnect());
    }

    public static void main(String[] args) {
        launch(args);
    }
}