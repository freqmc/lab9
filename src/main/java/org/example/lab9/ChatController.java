package org.example.lab9;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ChatController {
    @FXML private ComboBox<String> levelComboBox;
    @FXML private TextField nameField, recipientField, messageField;
    @FXML private Button connectBtn, sendBtn, inboxBtn;
    @FXML private TextArea logArea;
    @FXML private ListView<String> inboxListView;

    private final ChatModel model;
    public ChatController(ChatModel model) {
        this.model = model;
    }

    @FXML
    private void initialize() {
        if (model == null) {
            throw new IllegalStateException("Model cannot be null");
        }

        connectBtn.disableProperty().bind(model.connectedProperty());
        sendBtn.disableProperty().bind(model.connectedProperty().not());
        inboxBtn.disableProperty().bind(model.connectedProperty().not());
        nameField.disableProperty().bind(model.connectedProperty());

        logArea.textProperty().bind(model.logProperty());
        inboxListView.itemsProperty().bind(model.messagesProperty());
        levelComboBox.getItems().addAll(
                "Уровень 0: Без защиты (Direct)",
                "Уровень 1: Прокси (Hidden IP)",
                "Уровень 2: Туннель (ProtoBuf)",
                "Уровень 3: Прокси + Туннель (Max Security)"
        );
        levelComboBox.setValue("Уровень 0: Без защиты (Direct)");

        connectBtn.setOnAction(e -> {
            String level = levelComboBox.getValue();
            String name = nameField.getText().trim();
            if (!name.isEmpty()) model.connect(level, name);
            else model.appendLog("Ошибка: введите имя\n");
        });

        sendBtn.setOnAction(e -> {
            String to = recipientField.getText().trim();
            String msg = messageField.getText().trim();
            if (!to.isEmpty() && !msg.isEmpty()) {
                model.sendMessage(to, msg);
                Platform.runLater(() -> {
                    messageField.clear();
                    model.appendLog("Сообщение поставлено в очередь...\n");
                });
            }
        });

        inboxBtn.setOnAction(e -> {
            model.checkInbox();
        });
    }
}