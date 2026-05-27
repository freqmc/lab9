package org.example.lab9;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class ChatModel {
    private final StringProperty log = new SimpleStringProperty("");
    private final ObjectProperty<ObservableList<String>> messages =
            new SimpleObjectProperty<>(FXCollections.observableArrayList());
    private final BooleanProperty connected = new SimpleBooleanProperty(false);

    private Socket socket;
    private BufferedReader reader;
    private OutputStream rawOut;
    private SendStrategy strategy;
    private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, LevelConfig> securityLevels = new HashMap<>();

    public ChatModel() {
        securityLevels.put("Уровень 0: Без защиты (Direct)", new LevelConfig(5555, TextSendStrategy::new));
        securityLevels.put("Уровень 1: Прокси (Hidden IP)", new LevelConfig(5556, TextSendStrategy::new));
        securityLevels.put("Уровень 2: ProtoBuf туннель", new LevelConfig(5555, ProtoSendStrategy::new));
        securityLevels.put("Уровень 3: Прокси + ProtoBuf", new LevelConfig(5556, ProtoSendStrategy::new));
    }

    public StringProperty logProperty() { return log; }
    public ObjectProperty<ObservableList<String>> messagesProperty() { return messages; }
    public BooleanProperty connectedProperty() { return connected; }

    public void appendLog(String msg) {
        Platform.runLater(() -> log.set(log.get() + msg));
    }

    public void connect(String levelName, String username) {
        if (connected.get()) return;

        LevelConfig config = securityLevels.get(levelName);
        if (config == null) {
            appendLog("Ошибка: неизвестный уровень защиты\n");
            return;
        }

        new Thread(() -> {
            try {
                appendLog("Подключение к порту " + config.port + "...\n");
                socket = new Socket("localhost", config.port);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                rawOut = socket.getOutputStream();
                strategy = config.strategySupplier.get();

                connected.set(true);
                strategy.send(rawOut, "REGISTER|" + username);
                appendLog("Ожидание ответа сервера...\n");

                Thread readerThread = new Thread(this::startReaderLoop);
                readerThread.setDaemon(true);
                readerThread.start();

            } catch (IOException e) {
                appendLog("Ошибка подключения: " + e.getMessage() + "\n");
                disconnect();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void sendMessage(String recipient, String text) {
        if (!connected.get()) return;
        commandExecutor.submit(() -> {
            try {
                strategy.send(rawOut, "SEND|" + recipient + "|" + text);
            } catch (Exception e) {
                appendLog("Ошибка отправки: " + e.getMessage() + "\n");
            }
        });
    }

    public void checkInbox() {
        if (!connected.get()) return;
        commandExecutor.submit(() -> {
            try {
                strategy.send(rawOut, "INBOX");
            } catch (Exception e) {
                appendLog("Ошибка запроса: " + e.getMessage() + "\n");
            }
        });
    }

    public void assignSecretFriends() {
        if (!connected.get()) return;
        commandExecutor.submit(() -> {
            try {
                strategy.send(rawOut, "ASSIGN");
            } catch (Exception e) {
                appendLog("Ошибка назначения: " + e.getMessage() + "\n");
            }
        });
    }

    public void getHint() {
        if (!connected.get()) return;
        commandExecutor.submit(() -> {
            try {
                strategy.send(rawOut, "HINT");
            } catch (Exception e) {
                appendLog("Ошибка получения подсказки: " + e.getMessage() + "\n");
            }
        });
    }

    public void addHint(String hint) {
        if (!connected.get()) return;
        commandExecutor.submit(() -> {
            try {
                strategy.send(rawOut, "ADD_HINT|" + hint);
            } catch (Exception e) {
                appendLog("Ошибка добавления подсказки: " + e.getMessage() + "\n");
            }
        });
    }

    public void guessFriend(String guess) {
        if (!connected.get()) return;
        commandExecutor.submit(() -> {
            try {
                strategy.send(rawOut, "GUESS|" + guess);
            } catch (Exception e) {
                appendLog("Ошибка проверки: " + e.getMessage() + "\n");
            }
        });
    }

    public void disconnect() {
        connected.set(false);
        commandExecutor.shutdownNow();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private void startReaderLoop() {
        try {
            String line;
            while (connected.get() && (line = reader.readLine()) != null) {
                processResponse(line);
            }
            if (connected.get()) {
                appendLog("Сервер закрыл соединение.\n");
                Platform.runLater(this::disconnect);
            }
        } catch (IOException e) {
            appendLog("Потеряно соединение с сервером.\n");
            Platform.runLater(this::disconnect);
        }
    }

    private void processResponse(String response) {
        Platform.runLater(() -> {
            if (response.startsWith("OK|")) {
                appendLog("✅ " + response.substring(3) + "\n");
            } else if (response.startsWith("ERROR|")) {
                appendLog(" " + response.substring(6) + "\n");
            } else if (response.startsWith("MESSAGES|")) {
                ObservableList<String> currentList = messages.get();
                currentList.clear();
                String content = response.substring(9);
                if (!content.isEmpty()) {
                    for (String msg : content.split("\n")) {
                        if (!msg.trim().isEmpty()) currentList.add(msg);
                    }
                }
                appendLog("📥 Входящих: " + currentList.size() + "\n");
            } else if (response.startsWith("EMPTY|")) {
                messages.get().clear();
                appendLog(" " + response.substring(6) + "\n");
            } else if (response.startsWith("HINT|")) {
                appendLog("💡 ПОДСКАЗКА: " + response.substring(5) + "\n");
            } else if (response.startsWith("GUESS_RESULT|")) {
                appendLog("🎯 " + response.substring(13) + "\n");
            } else if (response.startsWith("PARTICIPANTS|")) {
                appendLog("👥 Участники: " + response.substring(13) + "\n");
            } else if (response.startsWith("REVEAL|")) {
                appendLog("🔓 РАСКРЫТИЕ: " + response.substring(7) + "\n");
            } else {
                appendLog("⚠️ " + response + "\n");
            }
        });
    }

    public static class LevelConfig {
        public final int port;
        public final Supplier<SendStrategy> strategySupplier;
        public LevelConfig(int port, Supplier<SendStrategy> strategySupplier) {
            this.port = port;
            this.strategySupplier = strategySupplier;
        }
    }

    public interface SendStrategy {
        void send(OutputStream out, String cmd) throws Exception;
    }

    // Текстовая стратегия (Уровни 0 и 1)
    public static class TextSendStrategy implements SendStrategy {
        @Override
        public void send(OutputStream out, String cmd) throws IOException {
            byte[] data = (cmd + "\n").getBytes(StandardCharsets.UTF_8);
            out.write(data);
            out.flush();
        }
    }

    // ProtoBuf стратегия (Уровни 2 и 3)
    public static class ProtoSendStrategy implements SendStrategy {
        // В методе send() класса ProtoSendStrategy:
        @Override
        public void send(OutputStream out, String cmd) throws IOException {
            String[] parts = cmd.split("\\|", 3);

            MessageProto.SecureMessage.Builder builder = MessageProto.SecureMessage.newBuilder();

            if (parts.length >= 1) {
                builder.setCommand(parts[0]);
            }
            if (parts.length >= 2) {
                if (parts[0].equals("SEND") && parts.length == 3) {
                    builder.setPayload(parts[1] + "|" + parts[2]);
                } else if (parts.length > 1) {
                    builder.setPayload(parts[1]);
                }
            }

            MessageProto.SecureMessage message = builder.build();
            byte[] data = message.toByteArray();

            // Отправляем: [общая длина 4 байта][ProtoBuf данные]
            byte[] lengthPrefix = ByteBuffer.allocate(4).putInt(data.length).array();
            out.write(lengthPrefix);
            out.write(data);
            out.flush();
        }
    }
}