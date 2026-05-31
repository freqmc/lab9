package org.example.lab9;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 5555;

    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> mailboxes = new ConcurrentHashMap<>();
    private static final List<String> gameParticipants = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<String, String> secretFriends = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Set<String>> hints = new ConcurrentHashMap<>();
    private static boolean gameStarted = false;

    private static final Map<String, Command> commandRegistry = new HashMap<>();

    static {
        registerCommands();
    }

    private static void registerCommands() {
        commandRegistry.put("REGISTER", (parts, out, ctx) -> {
            if (parts.length < 2) {
                out.println("ERROR|Не указано имя");
                return;
            }
            String username = parts[1];
            ctx.setUsername(username);
            mailboxes.putIfAbsent(username, new ConcurrentLinkedQueue<>());
            if (!gameParticipants.contains(username)) {
                gameParticipants.add(username);
            }
            hints.putIfAbsent(username, ConcurrentHashMap.newKeySet());
            out.println("OK|" + username + " зарегистрирован");
            System.out.println("Зарегистрирован: " + username);
        });

        commandRegistry.put("ASSIGN", (parts, out, ctx) -> {
            if (!gameStarted) {
                if (gameParticipants.size() < 2) {
                    out.println("ERROR|Нужно минимум 2 участника");
                    return;
                }

                List<String> shuffled = new ArrayList<>(gameParticipants);
                Collections.shuffle(shuffled);

                for (int i = 0; i < shuffled.size(); i++) {                    String giver = shuffled.get(i);
                    String receiver = shuffled.get((i + 1) % shuffled.size());
                    secretFriends.put(receiver, giver);

                    mailboxes.get(receiver).add("🎁 ВАШ ТАЙНЫЙ ДРУГ НАЗНАЧЕН! Начинайте делать приятности!");
                    System.out.println(giver + " -> тайный друг для -> " + receiver);
                }

                gameStarted = true;
                out.println("OK|Тайные друзья назначены! Игра началась.");
            } else {
                out.println("ERROR|Игра уже началась");
            }
        });

        commandRegistry.put("SEND", (parts, out, ctx) -> {
            if (parts.length < 3) {
                out.println("ERROR|Формат: SEND|Получатель|Текст");
                return;
            }
            String recipient = parts[1];
            String message = parts[2];

            if (!mailboxes.containsKey(recipient)) {
                out.println("ERROR|Пользователь " + recipient + " не найден");
            } else {
                mailboxes.get(recipient).add("🎄 " + message);
                out.println("OK|Сообщение отправлено");
            }
        });

        commandRegistry.put("HINT", (parts, out, ctx) -> {
            String username = ctx.getUsername();
            if (username == null) {
                out.println("ERROR|Сначала зарегистрируйтесь");
                return;
            }

            String mySecretFriend = secretFriends.get(username);
            if (mySecretFriend == null) {
                out.println("ERROR|Вам ещё не назначен тайный друг");
                return;
            }

            String hint = generateHint(mySecretFriend);
            out.println("HINT|" + hint);
        });
        commandRegistry.put("ADD_HINT", (parts, out, ctx) -> {
            if (parts.length < 2) {                out.println("ERROR|Формат: ADD_HINT|Текст подсказки");
                return;
            }
            String username = ctx.getUsername();
            if (username == null) {
                out.println("ERROR|Сначала зарегистрируйтесь");
                return;
            }
            String hint = parts[1];
            hints.get(username).add(hint);
            out.println("OK|Подсказка добавлена");
        });

        commandRegistry.put("GUESS", (parts, out, ctx) -> {
            if (parts.length < 2) {
                out.println("ERROR|Формат: GUESS|Имя друга");
                return;
            }
            String username = ctx.getUsername();
            if (username == null) {
                out.println("ERROR|Сначала зарегистрируйтесь");
                return;
            }
            String guess = parts[1];

            String actualFriend = secretFriends.get(username);
            if (actualFriend == null) {
                out.println("ERROR|Вам ещё не назначен тайный друг");
                return;
            }

            if (actualFriend.equals(guess)) {
                out.println("GUESS_RESULT|Поздравляем! Вы угадали! Ваш тайный друг - " + actualFriend);
                secretFriends.remove(username);
            } else {
                out.println("GUESS_RESULT|Неверно! Попробуйте ещё раз или возьмите подсказку (HINT)");
            }
        });

        commandRegistry.put("INBOX", (parts, out, ctx) -> {
            String user = ctx.getUsername();
            if (user == null) {
                out.println("ERROR|Сначала зарегистрируйтесь");
                return;
            }
            ConcurrentLinkedQueue<String> messages = mailboxes.get(user);
            if (messages == null || messages.isEmpty()) {
                out.println("EMPTY|Нет новых сообщений");
            } else {
                StringBuilder sb = new StringBuilder("MESSAGES|");                while (!messages.isEmpty()) {
                    sb.append(messages.poll()).append("\n");
                }
                if (sb.length() > 9) sb.setLength(sb.length() - 1);
                out.println(sb.toString());
            }
        });

        commandRegistry.put("LIST", (parts, out, ctx) -> {
            out.println("PARTICIPANTS|" + String.join(", ", gameParticipants));
        });

        commandRegistry.put("REVEAL", (parts, out, ctx) -> {
            StringBuilder sb = new StringBuilder("REVEAL|");
            for (Map.Entry<String, String> entry : secretFriends.entrySet()) {
                sb.append(entry.getKey()).append(" <- ").append(entry.getValue()).append("; ");
            }
            out.println(sb.toString());
        });
    }

    private static String generateHint(String secretFriend) {
        if (secretFriend == null || secretFriend.isEmpty()) {
            return "Подсказка недоступна";
        }

        char firstChar = secretFriend.charAt(0);
        int length = secretFriend.length();

        return "Первая буква: " + firstChar + ", длина имени: " + length + " букв";
    }

    public static void main(String[] args) {
        System.out.println("=== СЕРВЕР 'ТАЙНЫЙ ДРУГ' ЗАПУЩЕН ===");
        System.out.println("Порт: " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private SessionContext context = new SessionContext();

        public ClientHandler(Socket socket) {            this.socket = socket;
        }
        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            ) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    handleRequest(inputLine, out);
                }
            } catch (IOException e) {
                System.err.println("Ошибка соединения: " + e.getMessage());
            }
        }

        private void handleRequest(String commandLine, PrintWriter out) {
            String[] parts = commandLine.split("\\|", 3);
            if (parts.length == 0) return;

            String action = parts[0].toUpperCase();
            Command handler = commandRegistry.get(action);

            if (handler != null) {
                try {
                    handler.execute(parts, out, context);
                } catch (Exception e) {
                    out.println("ERROR|Внутренняя ошибка сервера");
                    e.printStackTrace();
                }
            } else {
                out.println("ERROR|Неизвестная команда: " + action);
            }
        }
    }
}