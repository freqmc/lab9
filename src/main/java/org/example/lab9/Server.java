package org.example.lab9;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 5555;
    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> mailboxes = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Socket> activeUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Сервер запущен на порту " + PORT);
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
        private String username = null;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            ) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    processCommand(inputLine, out);
                }
            } catch (IOException e) {
                System.err.println("Ошибка соединения с клиентом: " + e.getMessage());
            } finally {
                if (username != null) {
                    activeUsers.remove(username);
                    System.out.println("Пользователь " + username + " отключился.");
                }
            }
        }

        private void processCommand(String command, PrintWriter out) {
            String[] parts = command.split("\\|", 3);
            if (parts.length == 0) return;

            String action = parts[0].toUpperCase();

            switch (action) {
                case "REGISTER":
                    if (parts.length < 2) {
                        out.println("ERROR|Не указано имя");
                        return;
                    }
                    username = parts[1];
                    activeUsers.put(username, socket);
                    mailboxes.putIfAbsent(username, new ConcurrentLinkedQueue<>());
                    out.println("OK|" + username + " зарегистрирован");
                    System.out.println("Зарегистрирован: " + username);
                    break;

                case "SEND":
                    if (parts.length < 3) {
                        out.println("ERROR|Формат: SEND|Получатель|Текст");
                        return;
                    }
                    String recipient = parts[1];
                    String message = parts[2];

                    if (!mailboxes.containsKey(recipient)) {
                        out.println("ERROR|Пользователь " + recipient + " не найден");
                    } else {
                        mailboxes.get(recipient).add(message);
                        out.println("OK|Сообщение отправлено");
                        System.out.println("Сообщение доставлено в ящик " + recipient);
                    }
                    break;

                case "INBOX":
                    if (username == null) {
                        out.println("ERROR|Сначала зарегистрируйтесь");
                        return;
                    }
                    ConcurrentLinkedQueue<String> messages = mailboxes.get(username);
                    if (messages == null || messages.isEmpty()) {
                        out.println("EMPTY|Нет новых сообщений");
                    } else {
                        StringBuilder sb = new StringBuilder("MESSAGES|");
                        while (!messages.isEmpty()) {
                            sb.append(messages.poll()).append("\n");
                        }
                        if (sb.length() > 9) sb.setLength(sb.length() - 1);
                        out.println(sb.toString());
                    }
                    break;

                case "REVEAL":
                    out.println("INFO|Функция раскрытия авторов недоступна в анонимном режиме");
                    break;

                default:
                    out.println("ERROR|Неизвестная команда");
            }
        }
    }
}