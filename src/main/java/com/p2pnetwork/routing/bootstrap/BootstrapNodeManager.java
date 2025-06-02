package com.p2pnetwork.routing.bootstrap;

import com.p2pnetwork.message.*;
import com.p2pnetwork.network.MessageSender;
import com.p2pnetwork.node.Node;
import com.p2pnetwork.node.NodeRole;
import com.p2pnetwork.routing.RoutingEntry;

public class BootstrapNodeManager {
    public static boolean connect(Node newNode) {
        for (RoutingEntry bootstrap : BootstrapNodeTable.getAll()) {
            try {
                RoutingEntry myEntry = new RoutingEntry(
                        newNode.getNodeId(), newNode.getIp(), newNode.getPort(), NodeRole.PEER
                );
                Message<RoutingEntry> introduceMsg = new Message<>(
                        MessageType.INTRODUCE, newNode.getNodeId(), bootstrap.getNodeId(), myEntry, System.currentTimeMillis()
                );
                new MessageSender(newNode).sendMessage(bootstrap.getIp(), bootstrap.getPort(), introduceMsg);
                System.out.println("[INFO] 부트스트랩 노드 연결 성공: " + bootstrap.getNodeId());
                return true;
            } catch (Exception e) {
                System.err.println("[ERROR] 부트스트랩 노드 연결 실패: " + bootstrap.getNodeId());
            }
        }
        return false;
    }
}
