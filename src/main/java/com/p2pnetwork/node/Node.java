package com.p2pnetwork.node;

import com.p2pnetwork.message.Message;
import com.p2pnetwork.network.ClientHandler;
import com.p2pnetwork.network.MessageHandler;
import com.p2pnetwork.network.MessageSender;
import com.p2pnetwork.routing.table.LocalRoutingTable;
import com.p2pnetwork.routing.bootstrap.BootstrapNodeManager;
import com.p2pnetwork.routing.bootstrap.BootstrapNodeTable;
import lombok.Getter;

import java.io.IOException;
import java.net.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class Node {
    private final String nodeId;
    private final String ip;
    private final int port;
    private NodeRole role;

    private LocalRoutingTable localRoutingTable;
    private MessageHandler messageHandler;
    private MessageSender messageSender;

    private final ExecutorService executorService;

    public Node(int port) throws UnknownHostException {
        this.nodeId = UUID.randomUUID().toString(); // 다른 ID 생성 방식으로 수정 예정
        this.ip = InetAddress.getLocalHost().getHostAddress();
        this.port = port;
        this.role = NodeRole.PEER;

        this.localRoutingTable = new LocalRoutingTable(this);
        this.messageHandler = new MessageHandler(this);
        this.messageSender = new MessageSender(this);

        this.executorService = Executors.newFixedThreadPool(10);
    }

    public void start() {
        System.out.println("Node started at port " + port + " with ID " + nodeId);
        executorService.submit(this::listen);  // 스레드 풀에서 listen 메소드 실행

        if (!BootstrapNodeTable.isPortBootstrapNode(port)) {
            connectToBootstrap();
        }
    }

    private void listen() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket client = serverSocket.accept();
                executorService.submit(new ClientHandler(client, this));  // 클라이언트 연결을 스레드 풀에서 처리
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectToBootstrap() {
        boolean connected = BootstrapNodeManager.connectToBootstrap(this);
        if (!connected) {
            System.err.println("모든 부트스트랩 노드에 대해 연결이 실패하였습니다.");
            shutdown();
            System.exit(1);
        }
    }

    public <T> void receiveMessage(Message<T> message) {
        messageHandler.handleReceivedMessage(message);
    }

    public LocalRoutingTable getRoutingTable() {
        return localRoutingTable;
    }

    public boolean hasAtLeastRole(NodeRole requiredRole) {
        return this.role.isAtLeast(requiredRole);
    }

    public void shutdown() {
        executorService.shutdown();  // 스레드 풀 종료
    }
}
