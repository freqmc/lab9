package org.example.lab9;

import java.io.*;
import java.net.*;

public class ProxyServer {
    private static final int PROXY_PORT = 5556;
    private static final String TARGET_HOST = "localhost";
    private static final int TARGET_PORT = 5555;

    public static void main(String[] args) {
        System.out.println("Прокси-сервер запущен на порту " + PROXY_PORT);
        System.out.println("Перенаправление на " + TARGET_HOST + ":" + TARGET_PORT);

        try (ServerSocket proxySocket = new ServerSocket(PROXY_PORT)) {
            while (true) {
                Socket clientSocket = proxySocket.accept();
                System.out.println("Клиент подключился к прокси: " + clientSocket.getInetAddress());
                new Thread(() -> handleProxyConnection(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Ошибка запуска прокси-сервера: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleProxyConnection(Socket clientSocket) {
        try (
                Socket targetSocket = new Socket(TARGET_HOST, TARGET_PORT);
                InputStream clientIn = clientSocket.getInputStream();
                OutputStream clientOut = clientSocket.getOutputStream();
                InputStream targetIn = targetSocket.getInputStream();
                OutputStream targetOut = targetSocket.getOutputStream();
        ) {
            Thread t1 = new Thread(() -> transferStream(clientIn, targetOut, "C->S"));
            Thread t2 = new Thread(() -> transferStream(targetIn, clientOut, "S->C"));

            t1.start();
            t2.start();

            t1.join();
            t2.join();

        } catch (Exception e) {
            System.err.println("Ошибка в туннеле прокси: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            } catch (IOException ignored) {}
        }
    }

    private static void transferStream(InputStream from, OutputStream to, String label) {
        byte[] buffer = new byte[4096];
        int bytesRead;
        try {
            while ((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead);
                to.flush();
            }
        } catch (IOException e) {
            // Соединение разорвано
        }
    }
}