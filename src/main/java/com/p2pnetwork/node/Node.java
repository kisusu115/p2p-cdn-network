package com.p2pnetwork.node;

import com.p2pnetwork.message.Message;
import com.p2pnetwork.message.MessageType;
import com.p2pnetwork.network.ClientHandler;
import com.p2pnetwork.network.MessageHandler;
import com.p2pnetwork.network.MessageSender;
import com.p2pnetwork.routing.RoutingEntry;
import com.p2pnetwork.routing.bootstrap.BootstrapNodeManager;
import com.p2pnetwork.routing.bootstrap.BootstrapNodeTable;
import com.p2pnetwork.routing.table.LocalRoutingTable;
import com.p2pnetwork.util.NodeIdGenerator;
import lombok.Getter;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class Node {
    private final String nodeId;
    private final String ip;
    private final int port;
    private NodeRole role;
    private final LocalRoutingTable routingTable;
    private final ExecutorService executor;
    private final MessageHandler messageHandler;
    private final MessageSender messageSender;

    private Socket TCPSocket = null;
    public void setTCPSocket(Socket TCPSocket) {
        this.TCPSocket = TCPSocket;
    }

    public Node(double lat, double lon, int port) throws Exception {
        this.ip = InetAddress.getLocalHost().getHostAddress();;
        this.port = port;
        this.nodeId = NodeIdGenerator.generateNodeId(lat, lon, ip, port);
        this.role = NodeRole.PEER;
        this.routingTable = new LocalRoutingTable(this);
        this.executor = Executors.newFixedThreadPool(10);
        this.messageHandler = new MessageHandler(this);
        this.messageSender = new MessageSender(this);
    }

    public void start() {
        executor.submit(this::listen);

        if (isBootstrap()) {
            this.role = NodeRole.BOOTSTRAP;
        } else if (isRedundancy()) {
            this.role = NodeRole.REDUNDANCY;
            boolean connected = BootstrapNodeManager.connectRedundancy(this);
            if (!connected) {
                System.err.println("[ERROR] 대응되는 부트스트랩 노드에 연결할 수 없습니다. 프로그램을 종료합니다.");
                System.exit(1);
            }
        } else {
            boolean connected = BootstrapNodeManager.connect(this);
            if (!connected) {
                System.err.println("[ERROR] 부트스트랩 노드에 연결할 수 없습니다. 프로그램을 종료합니다.");
                System.exit(1);
            }
        }
    }

    private void listen() {
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) executor.submit(new ClientHandler(server.accept(), this));
        } catch (Exception e) {
            if (this.role == NodeRole.SUPERNODE){
                messageSender.sendMessage(this.routingTable.getRedundancyEntry(), new Message<>(
                        MessageType.SERVERSOCKET_DEAD,
                        nodeId,
                        routingTable.getRedundancyEntry().getNodeId(),
                        null,
                        System.currentTimeMillis()));
            }
            System.err.println("[ERROR] 포트 " + port + " 리스닝 실패: " + e.getMessage());
            throw new RuntimeException("포트 " + port + " 리스닝 실패", e);
        }
    }

    private boolean isBootstrap() {
        String geohash5 = nodeId.split("_")[0];
        RoutingEntry bootstrap = BootstrapNodeTable.getBootstrapByGeohash(geohash5);
        return bootstrap != null && port >= 10001 && port <= 10004;
    }

    private boolean isRedundancy() {
        String geohash5 = nodeId.split("_")[0];
        RoutingEntry bootstrap = BootstrapNodeTable.getBootstrapByGeohash(geohash5);
        return bootstrap != null && port >= 10005 && port <= 10008;
    }

    public void receiveMessage(Message<?> message) {
        messageHandler.handleReceivedMessage(message);
    }

    public boolean hasAtLeastRole(NodeRole role) {
        return this.role.isAtLeast(role);
    }

    public void promoteToSuperNode() {
        this.role = NodeRole.SUPERNODE;
        System.out.println("[INFO] " + nodeId + " ▶ SUPERNODE 승격");
        RoutingEntry entry = new RoutingEntry(nodeId, ip, port, NodeRole.SUPERNODE);

        // 본인을 LocalRoutingTable의 SuperNodeEntry 지정 및 로컬 라우팅테이블에 추가
        this.routingTable.setSuperNodeEntry(entry);
        this.routingTable.addEntry(entry);
    }

    public void setSuperNode(RoutingEntry superNodeEntry) {
        this.routingTable.setSuperNodeEntry(superNodeEntry);

        // 슈퍼노드에 JOIN_REQUEST 전송
        RoutingEntry myEntry = new RoutingEntry(
                nodeId, ip, port, NodeRole.PEER
        );
        Message<RoutingEntry> joinRequest = new Message<>(
                MessageType.JOIN_REQUEST,
                nodeId,
                superNodeEntry.getNodeId(),
                myEntry,
                System.currentTimeMillis()
        );
        messageSender.sendMessage(
                superNodeEntry.getIp(),
                superNodeEntry.getPort(),
                joinRequest
        );
    }

    public void setRole(NodeRole role) {
        this.role = role;
        System.out.println("[INFO] " + nodeId + " ▶ NodeRole 변경");
    }

    public void promoteToRedundancy() {
        this.role = NodeRole.REDUNDANCY;
    }
}
