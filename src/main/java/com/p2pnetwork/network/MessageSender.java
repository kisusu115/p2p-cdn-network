package com.p2pnetwork.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2pnetwork.message.Message;
import com.p2pnetwork.node.Node;
import com.p2pnetwork.routing.RoutingEntry;
import com.p2pnetwork.util.JsonUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class MessageSender {
    private final Node node;
    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson 사용

    public MessageSender(Node node) {
        this.node = node;
    }

    public void sendMessage(Socket socket, Message<?> message) {
        sendSocketMessage(socket, message);
    }

    public void sendMessage(String nodeId, Message<?> message) {
        RoutingEntry entry = node.getRoutingTable()
                .getEntries()
                .stream()
                .filter(e -> e.getNodeId().equals(nodeId))
                .findFirst()
                .orElse(null);

        if (entry == null) {
            System.err.println("Node ID " + nodeId + "는 로컬라우팅 테이블에 존재하지 않습니다.");
            return;
        }

        sendMessage(entry.getIp(), entry.getPort(), message);
    }

    public void sendMessage(String ip, int port, Message<?> message) {
        new Thread(() -> {
            try (Socket socket = new Socket(ip, port)) {
                sendSocketMessage(socket, message);
            } catch (IOException e) {
                System.err.println("Failed to send JSON message to " + ip + ":" + port + " - " + e.getMessage());
            }
        }).start();
    }

    private void sendSocketMessage(Socket socket, Message<?> message) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"))) {
            String json = JsonUtils.toJson(message);  // JsonUtils 사용
            writer.write(json);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Failed to send JSON message via Socket: " + e.getMessage());
        }
    }
}
