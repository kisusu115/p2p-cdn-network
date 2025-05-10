package com.p2pnetwork.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.p2pnetwork.message.Message;
import com.p2pnetwork.node.Node;
import com.p2pnetwork.util.JsonUtils;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Node node;

    public ClientHandler(Socket socket, Node node) {
        this.socket = socket;
        this.node = node;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
            String jsonMessage = reader.readLine();

            // TypeReference 사용하여 제네릭 타입 처리
            Message<?> message = JsonUtils.fromJson(jsonMessage, new TypeReference<Message<?>>() {});

            if (message != null) {
                node.receiveMessage(message);  // 받은 메시지를 Node로 전달
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

