package com.p2pnetwork.routing.bootstrap;

import com.p2pnetwork.message.Message;
import com.p2pnetwork.message.dto.IntroduceContent;
import com.p2pnetwork.node.Node;
import com.p2pnetwork.routing.RoutingEntry;

import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;

import static com.p2pnetwork.message.MessageType.INTRODUCE;

public class BootstrapNodeManager {

    public static boolean connectToBootstrap(Node node) {
        Iterator<Map.Entry<String, RoutingEntry>> iterator = BootstrapNodeTable.staticBootstrapMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, RoutingEntry> entry = iterator.next();
            RoutingEntry bootstrapNode = entry.getValue();

            try (Socket socket = new Socket(bootstrapNode.getIp(), bootstrapNode.getPort())) {
                IntroduceContent content = IntroduceContent.of(node.getIp(), node.getPort());
                Message<IntroduceContent> message = Message.of(INTRODUCE, node.getNodeId(), bootstrapNode.getNodeId(), content);
                node.getMessageSender().sendMessage(socket, message);
                return true;

            } catch (IOException e) {
                System.err.println("Failed to connect to Bootstrap node: " + bootstrapNode.getNodeId());
            }
        }

        return false; // 모든 부트스트랩 노드 연결 실패
    }
}
