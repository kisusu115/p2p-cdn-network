package com.p2pnetwork.routing.bootstrap;

import com.p2pnetwork.message.*;
import com.p2pnetwork.network.MessageSender;
import com.p2pnetwork.node.Node;
import com.p2pnetwork.node.NodeRole;
import com.p2pnetwork.routing.RoutingEntry;

import java.net.Socket;

public class BootstrapNodeManager {

    public static boolean connect(Node newNode) {
        for (RoutingEntry bootstrap : BootstrapNodeTable.getAll()) {
            try {
                RoutingEntry myEntry = new RoutingEntry(
                        newNode.getNodeId(), newNode.getIp(), newNode.getPort(), NodeRole.PEER
                );
                Message<RoutingEntry> introduceMsg = new Message<>(
                        MessageType.INTRODUCE,
                        newNode.getNodeId(),
                        bootstrap.getNodeId(),
                        myEntry,
                        System.currentTimeMillis()
                );
                Socket socket = new Socket(bootstrap.getIp(), bootstrap.getPort());
                new MessageSender(newNode).sendSocketMessage(socket, introduceMsg);
                System.out.println("[INFO] 부트스트랩 노드 연결 성공: " + bootstrap.getNodeId());
                return true;
            } catch (Exception e) {
                System.err.println("[ERROR] 부트스트랩 노드 연결 실패: " + bootstrap.getNodeId());
            }
        }
        return false;
    }

    public static boolean connectRedundancy(Node node) {
        try {
            String geohash5 = node.getNodeId().split("_")[0];
            RoutingEntry bootstrapEntry = BootstrapNodeTable.getBootstrapByGeohash(geohash5);

            if (bootstrapEntry == null) {
                System.err.println("[ERROR] geohash5 '" + geohash5 + "' 에 해당하는 Bootstrap 노드가 없습니다.");
                return false;
            }

            RoutingEntry myEntry = new RoutingEntry(node.getNodeId(), node.getIp(), node.getPort(), NodeRole.REDUNDANCY);
            Message<RoutingEntry> introduceMsg = new Message<>(
                    MessageType.INTRODUCE,
                    node.getNodeId(),
                    bootstrapEntry.getNodeId(),
                    myEntry,
                    System.currentTimeMillis()
            );

            new MessageSender(node).sendMessage(bootstrapEntry.getIp(), bootstrapEntry.getPort(), introduceMsg);
            System.out.println("[INFO] Redundancy 노드가 Bootstrap에 INTRODUCE 전송: " + bootstrapEntry.getNodeId());
            return true;
        } catch (Exception e) {
            System.err.println("[ERROR] Redundancy Bootstrap 연결 실패");
            return false;
        }
    }
}
