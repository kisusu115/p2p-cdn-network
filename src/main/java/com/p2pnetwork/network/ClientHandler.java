package com.p2pnetwork.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.p2pnetwork.message.Message;
import com.p2pnetwork.message.MessageType;
import com.p2pnetwork.node.Node;
import com.p2pnetwork.node.NodeRole;
import com.p2pnetwork.routing.RoutingEntry;
import com.p2pnetwork.routing.table.SuperNodeTable;
import com.p2pnetwork.util.JsonUtils;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Node node;
    public ClientHandler(Socket socket, Node node) {
        this.socket = socket; this.node = node;
    }
    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
            String jsonMessage = reader.readLine();
            Message<?> message = JsonUtils.fromJson(jsonMessage, new TypeReference<Message<?>>() {});

            if (message == null) {
                System.out.println("[ERROR] 메시지가 비어있습니다.");
                return;
            }

            if (message.getType() != MessageType.TCP_CONNECT){
                node.receiveMessage(message);
                socket.close();
                //System.out.println("[MHED] Message Handler End: " + message.getType() + " 메시지 처리 종료");
                return;
            }

            // MessageType.TCP_CONNECT 인 경우 시행 로직
            System.out.println("[RECV] " + message.getType() + " from " + message.getSenderId());
            node.setTCPSocket(socket);
            /*System.out.println("[INFO] SuperNode " + SuperNodeTable.getInstance().getSuperNode(node.getNodeId().split("_")[0]).getNodeId()
                    + "와 TCP 연결이 수립되었습니다.");
            System.out.println("[INFO] SuperNode " + node.getRoutingTable().getSuperNodeEntry().getNodeId()
                    + "와 TCP 연결이 수립되었습니다.");*/
            System.out.println("[INFO] SuperNode와 TCP 연결 수립 완료");

            while (true) {
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                int data = br.read();
                if (data == -1) {
                    /*System.out.println("Log: -1");

                    System.out.println("[ERROR] SuperNode와 연결이 끊어졌습니다. 승격을 요청합니다.");*/
                    break;
                }
            }

            //System.out.println("Log: Socket Death");
            // TODO: Redundancy가 SuperNode에 문제를 감지 → 승격 요청 --> 완료
            /*String geohash5 = node.getNodeId().split("_")[0];
            RoutingEntry superEntry = node.getRoutingTable().getSuperNodeEntry();


            MessageSender sender = new MessageSender(node);
            if (superEntry.getRole() == NodeRole.BOOTSTRAP) {
                Message<String> broadcastMsg = new Message<String>(
                        MessageType.REQUEST_TEMP_PROMOTE,
                        node.getNodeId(),
                        "ALL",
                        geohash5,
                        System.currentTimeMillis()
                );
                SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                        .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                        .forEach(entry -> sender.sendMessage(entry.getIp(), entry.getPort(), broadcastMsg));
            }
            else {

                Message<String> broadcastMsg = new Message<String>(
                        MessageType.REQUEST_PROMOTE,
                        node.getNodeId(),
                        "ALL",
                        geohash5,
                        System.currentTimeMillis()
                );
                SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                        .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                        .forEach(entry -> sender.sendMessage(entry.getIp(), entry.getPort(), broadcastMsg));
            }*/

        } catch (SocketException e){
            System.out.println("[ERROR] SuperNode와 연결이 끊어졌습니다. 승격을 요청합니다.");
            String geohash5 = node.getNodeId().split("_")[0];
            RoutingEntry superEntry = node.getRoutingTable().getSuperNodeEntry();

            AtomicInteger superCount = new AtomicInteger(0);
            node.getMessageHandler().setSuperCount(superCount.get());
            for (RoutingEntry entry : SuperNodeTable.getInstance().getAllSuperNodeEntries()){
                new Thread(() -> {
                    Socket socket = new Socket();
                    try {
                        SocketAddress address = new InetSocketAddress(entry.getIp(), entry.getPort());
                        socket.connect(address, 1000);
                        superCount.getAndIncrement();
                        node.getMessageHandler().setSuperCount(superCount.get());
                    } catch (Exception ex){
                        System.out.println("[INFO] " + entry.getNodeId() + " 죽음 확인");
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException ex) {
                            System.out.println("[ERROR] Socket Closing Error");
                        }
                    }
                }).start();
            }

            MessageSender sender = new MessageSender(node);
            if (superEntry.getRole() == NodeRole.BOOTSTRAP) {
                Message<String> broadcastMsg = new Message<String>(
                        MessageType.REQUEST_TEMP_PROMOTE,
                        node.getNodeId(),
                        "ALL",
                        geohash5,
                        System.currentTimeMillis()
                );
                SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                        .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                        .forEach(entry -> sender.sendMessage(entry.getIp(), entry.getPort(), broadcastMsg));
            }
            else {
                 Message<String> broadcastMsg = new Message<String>(
                        MessageType.REQUEST_PROMOTE,
                        node.getNodeId(),
                        "ALL",
                        geohash5,
                        System.currentTimeMillis()
                );
                SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                        .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                        .forEach(entry -> sender.sendMessage(entry.getIp(), entry.getPort(), broadcastMsg));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}