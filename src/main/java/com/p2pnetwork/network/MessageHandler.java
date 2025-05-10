package com.p2pnetwork.network;

import com.p2pnetwork.message.Message;
import com.p2pnetwork.message.dto.IntroduceContent;
import com.p2pnetwork.node.Node;
import static com.p2pnetwork.node.NodeRole.BOOTSTRAP;

public class MessageHandler {

    private final Node node;

    public MessageHandler(Node node) {
        this.node = node;
    }

    public void handleReceivedMessage(Message<?> message) {
        switch (message.getType()) {
            case INTRODUCE:
                if (!node.hasAtLeastRole(BOOTSTRAP)) {
                    // TODO 내가 부트스트랩 노드가 아니기에, 잘못된 요청이라고 응답 처리
                    return;
                }
                IntroduceContent content = (IntroduceContent) message.getContent();
                System.out.println("새로운 Peer 접근: " + content.getIp() + ":" + content.getPort());
                // TODO 다른 슈퍼 노드들에게 RTT_CHECK_REQUEST 전송
                break;
            case DIRECT_MESSAGE:
                // 다른 메시지 처리 로직
                break;
            default:
                break;
        }
    }

}
