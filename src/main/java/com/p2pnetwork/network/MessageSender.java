package com.p2pnetwork.network;

import com.p2pnetwork.message.Message;
import com.p2pnetwork.message.MessageType;
import com.p2pnetwork.node.Node;
import com.p2pnetwork.routing.RoutingEntry;
import com.p2pnetwork.util.JsonUtils;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class MessageSender {
    private final Node node;
    public MessageSender(Node node) { this.node = node; }

    public void sendMessage(String nodeId, Message<?> message) {
        RoutingEntry entry = node.getRoutingTable().getEntries().stream()
                .filter(e -> e.getNodeId().equals(nodeId)).findFirst().orElse(null);
        if (entry == null) {
            System.err.println("[ERROR] Node ID " + nodeId + "는 로컬라우팅 테이블에 존재하지 않습니다.");
            return;
        }
        sendMessage(entry.getIp(), entry.getPort(), message);
    }

    public void sendMessage(RoutingEntry routingEntry, Message<?> message) {
        sendMessage(routingEntry.getIp(), routingEntry.getPort(), message);
    }

    public void sendMessage(String ip, int port, Message<?> message) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(ip, port);
                if (message.getType() == MessageType.TCP_CONNECT) {
                    node.setTCPSocket(socket);
                }
                sendSocketMessage(socket, message);
            } catch (IOException e) {
                System.err.println("[ERROR] " + message.getType() + " 전송 실패: " + e.getMessage());
            }
        }).start();
    }

    public void sendSocketMessage(Socket socket, Message<?> message) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"))) {
            String json = JsonUtils.toJson(message);
            writer.write(json);
            writer.newLine();
            writer.flush();
            System.out.println("[SEND] " + message.getType() + " to " + message.getTargetId());

            if (message.getType() == MessageType.TCP_CONNECT) {
                try {
                    while (true) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        int data = br.read();
                        if (data == -1) {
                            break;
                        }
                    }
                } catch (SocketException e){
                    this.sendMessage(node.getIp(), node.getPort(), new Message<>(
                            MessageType.REDUNDANCY_DISCONNECT,
                            node.getNodeId(),
                            node.getNodeId(),
                            null,
                            System.currentTimeMillis()
                    ));
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] 메시지 직렬화 실패: " + e.getMessage());
        }
    }
}