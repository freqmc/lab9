package org.example.lab9;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ChatController {
    @FXML private ComboBox<String> levelComboBox;
    @FXML private TextField nameField, recipientField, messageField;
    @FXML private TextField guessField, addHintField;
    @FXML private Button connectBtn, sendBtn, inboxBtn;
    @FXML private Button assignBtn, hintBtn, guessBtn, addHintBtn;
    @FXML private TextArea logArea;
    @FXML private ListView<String> inboxListView;

    private ChatModel model;

    public ChatController(ChatModel model) {
        this.model = model;
    }

    @FXML
    private void initialize() {
        if (model == null) throw new IllegalStateException("Model cannot be null");

        // Привязки
        connectBtn.disableProperty().bind(model.connectedProperty());
        sendBtn.disableProperty().bind(model.connectedProperty().not());
        inboxBtn.disableProperty().bind(model.connectedProperty().not());
        nameField.disableProperty().bind(model.connectedProperty());

        // Кнопки игры тоже активируются после подключения
        assignBtn.disableProperty().bind(model.connectedProperty().not());
        hintBtn.disableProperty().bind(model.connectedProperty().not());
        guessBtn.disableProperty().bind(model.connectedProperty().not());
        addHintBtn.disableProperty().bind(model.connectedProperty().not());

        logArea.textProperty().bind(model.logProperty());
        inboxListView.itemsProperty().bind(model.messagesProperty());

        // Заполнение ComboBox
        levelComboBox.getItems().addAll(
                "Уровень 0: Без защиты (Direct)",
                "Уровень 1: Прокси (Hidden IP)",
                "Уровень 2: Туннель (ProtoBuf)",
                "Уровень 3: Прокси + Туннель (Max Security)"
        );
        levelComboBox.setValue("Уровень 0: Без защиты (Direct)");

        // Обработчики
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
                    model.appendLog("Сообщение отправлено\n");
                });
            }
        });

        inboxBtn.setOnAction(e -> model.checkInbox());

        // === НОВЫЕ ОБРАБОТЧИКИ ДЛЯ ИГРЫ ===

        assignBtn.setOnAction(e -> {
            model.appendLog("🎲 Назначение тайных друзей...\n");
            model.assignSecretFriends();
        });

        hintBtn.setOnAction(e -> {
            model.appendLog("💡 Запрос подсказки...\n");
            model.getHint();
        });

        guessBtn.setOnAction(e -> {
            String guess = guessField.getText().trim();
            if (!guess.isEmpty()) {
                model.appendLog("🎯 Проверка: " + guess + "\n");
                model.guessFriend(guess);
                guessField.clear();
            } else {
                model.appendLog("Введите имя для проверки\n");
            }
        });

        addHintBtn.setOnAction(e -> {
            String hint = addHintField.getText().trim();
            if (!hint.isEmpty()) {
                model.appendLog("➕ Добавление подсказки...\n");
                model.addHint(hint);
                addHintField.clear();
            } else {
                model.appendLog("Введите текст подсказки\n");
            }
        });
    }
}