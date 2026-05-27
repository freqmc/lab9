package org.example.lab9;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Упрощённая реализация ProtoBuf сообщения без генерации.
 * Формат: [длина 4 байта][command длина 4 байта][command][payload длина 4 байта][payload]
 */
public class MessageProto {

    public static class SecureMessage {
        private String command = "";
        private String payload = "";

        public static Builder newBuilder() {
            return new Builder();
        }

        public byte[] toByteArray() {
            try {
                byte[] commandBytes = command.getBytes(StandardCharsets.UTF_8);
                byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

                // Вычисляем общую длину
                int totalLength = 4 + commandBytes.length + 4 + payloadBytes.length;

                ByteBuffer buffer = ByteBuffer.allocate(totalLength);
                buffer.putInt(commandBytes.length);
                buffer.put(commandBytes);
                buffer.putInt(payloadBytes.length);
                buffer.put(payloadBytes);

                return buffer.array();
            } catch (Exception e) {
                throw new RuntimeException("Ошибка сериализации", e);
            }
        }

        public static SecureMessage parseFrom(byte[] data) throws InvalidProtocolBufferException {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(data);

                int commandLength = buffer.getInt();
                byte[] commandBytes = new byte[commandLength];
                buffer.get(commandBytes);
                String command = new String(commandBytes, StandardCharsets.UTF_8);

                int payloadLength = buffer.getInt();
                byte[] payloadBytes = new byte[payloadLength];
                buffer.get(payloadBytes);
                String payload = new String(payloadBytes, StandardCharsets.UTF_8);

                SecureMessage message = new SecureMessage();
                message.command = command;
                message.payload = payload;
                return message;
            } catch (Exception e) {
                throw new InvalidProtocolBufferException("Ошибка десериализации: " + e.getMessage());
            }
        }

        public String getCommand() {
            return command;
        }

        public String getPayload() {
            return payload;
        }

        public static class Builder {
            private final SecureMessage message = new SecureMessage();

            public Builder setCommand(String command) {
                message.command = command;
                return this;
            }

            public Builder setPayload(String payload) {
                message.payload = payload;
                return this;
            }

            public SecureMessage build() {
                return message;
            }
        }
    }
}