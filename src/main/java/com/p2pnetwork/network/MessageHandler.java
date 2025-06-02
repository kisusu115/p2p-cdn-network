package com.p2pnetwork.network;

import com.p2pnetwork.message.*;
import com.p2pnetwork.message.dto.AssignSuperNodeContent;
import com.p2pnetwork.message.dto.JoinResponseContent;
import com.p2pnetwork.node.Node;
import com.p2pnetwork.node.NodeRole;
import com.p2pnetwork.routing.RoutingEntry;
import com.p2pnetwork.routing.table.SuperNodeTable;

import java.util.Arrays;

public class MessageHandler {
    private final Node node;

    public MessageHandler(Node node) { this.node = node; }

    public void handleReceivedMessage(Message<?> message) {
        // 수신 로그 통일
        System.out.println("[RECV] " + message.getType() + " from " + message.getSenderId());
        switch (message.getType()) {
            case INTRODUCE:
                handleIntroduce(message);
                break;
            case ASSIGN_SUPERNODE:
                handleAssignSuperNode(message);
                break;
            case NEW_SUPERNODE_BROADCAST:
                handleNewSuperNodeBroadcast(message);
                break;
            case JOIN_REQUEST:
                handleJoinRequest(message);
                break;
            case JOIN_RESPONSE:
                handleJoinResponse(message);
                break;
            case NEW_PEER_BROADCAST:
                handleNewPeerBroadcast(message);
                break;
            case PROMOTE_REDUNDANCY:
                handlePromoteRedundancy(message);
                break;
            case NEW_REDUNDANCY_BROADCAST:
                handleNewRedundancyBroadcast(message);
                break;
            default:
                break;
        }
    }

    // 부트스트랩 노드에서만 동작
    private void handleIntroduce(Message<?> message) {
        if (!node.hasAtLeastRole(NodeRole.BOOTSTRAP)) return;
        RoutingEntry peerEntry = (RoutingEntry) message.getContent();
        String geohash5 = peerEntry.getNodeId().split("_")[0];
        RoutingEntry existing = SuperNodeTable.getInstance().getSuperNode(geohash5);

        MessageSender sender = new MessageSender(node);
        if (existing == null) {
            // 슈퍼노드 승격
            SuperNodeTable.getInstance().addSuperNode(peerEntry);
            AssignSuperNodeContent content = new AssignSuperNodeContent(
                    true,
                    peerEntry,
                    SuperNodeTable.getInstance().getAllSuperNodeEntries().toArray(new RoutingEntry[0])
            );
            // ASSIGN_SUPERNODE 메시지 전송
            sender.sendMessage(
                    peerEntry.getIp(),
                    peerEntry.getPort(),
                    new Message<>(
                            MessageType.ASSIGN_SUPERNODE,
                            node.getNodeId(),
                            peerEntry.getNodeId(),
                            content,
                            System.currentTimeMillis()
                    )
            );

            // NEW_SUPERNODE_BROADCAST 메시지 브로드캐스트
            Message<RoutingEntry> broadcastMsg = new Message<>(
                    MessageType.NEW_SUPERNODE_BROADCAST,
                    node.getNodeId(),
                    "ALL",
                    peerEntry,
                    System.currentTimeMillis()
            );
            SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                    .filter(entry -> !entry.getNodeId().equals(peerEntry.getNodeId())) // 새로 등록된 슈퍼노드 제외
                    .forEach(entry -> sender.sendMessage(entry.getIp(), entry.getPort(), broadcastMsg));
        } else {
            // 기존 슈퍼노드 할당
            AssignSuperNodeContent content = new AssignSuperNodeContent(
                    false,
                    existing,
                    null
            );
            sender.sendMessage(
                    peerEntry.getIp(),
                    peerEntry.getPort(),
                    new Message<>(
                            MessageType.ASSIGN_SUPERNODE,
                            node.getNodeId(),
                            peerEntry.getNodeId(),
                            content,
                            System.currentTimeMillis()
                    )
            );
        }
    }

    private void handleAssignSuperNode(Message<?> message) {
        AssignSuperNodeContent content = (AssignSuperNodeContent) message.getContent();
        if (content.isPromote()) {
            node.promoteToSuperNode();
            System.out.println("[INFO] " + node.getNodeId() + " ▶ 새로운 슈퍼노드로 승격되었습니다.");
            if (content.getSuperNodeTable() != null) {
                Arrays.stream(content.getSuperNodeTable())
                        .forEach(SuperNodeTable.getInstance()::addSuperNode);
                System.out.println("[INFO] 슈퍼노드 테이블 갱신");
            }
        } else {
            node.setSuperNode(content.getSuperNodeEntry());
            System.out.println("[INFO] " + node.getNodeId() + " ▶ 슈퍼노드 " +
                    content.getSuperNodeEntry().getNodeId() + " 에 할당되었습니다.");
        }
    }

    private void handleNewSuperNodeBroadcast(Message<?> message) {
        RoutingEntry newSuperNode = (RoutingEntry) message.getContent();
        System.out.println("[INFO] " + newSuperNode.getNodeId() + " ▶ 새로운 슈퍼노드 등록");
        SuperNodeTable.getInstance().addSuperNode(newSuperNode);
    }

    private void handleJoinRequest(Message<?> message) {
        RoutingEntry peerEntry = (RoutingEntry) message.getContent();
        node.getRoutingTable().addEntry(peerEntry);

        boolean isFirstPeer = node.getRoutingTable().getEntries().size() == 2; // 본인+peer

        if (isFirstPeer) {
            peerEntry.setRole(NodeRole.REDUNDANCY);
            node.getRoutingTable().setRedundancyEntry(peerEntry);

            Message<Void> promoteMsg = new Message<>(
                    MessageType.PROMOTE_REDUNDANCY,
                    node.getNodeId(),
                    peerEntry.getNodeId(),
                    null,
                    System.currentTimeMillis()
            );
            new MessageSender(node).sendMessage(peerEntry.getIp(), peerEntry.getPort(), promoteMsg);

            Message<RoutingEntry> broadcastMsg = new Message<>(
                    MessageType.NEW_REDUNDANCY_BROADCAST,
                    node.getNodeId(),
                    "ALL",
                    peerEntry,
                    System.currentTimeMillis()
            );
            SuperNodeTable.getInstance().getAllSuperNodeEntries()
                    .forEach(entry -> {
                        new MessageSender(node).sendMessage(entry.getIp(), entry.getPort(), broadcastMsg);
                    });
        }

        JoinResponseContent respContent = new JoinResponseContent(
                node.getRoutingTable().getSuperNodeEntry(),
                node.getRoutingTable().getRedundancyEntry(),
                node.getRoutingTable().getEntries().toArray(new RoutingEntry[0])
        );
        Message<JoinResponseContent> resp = new Message<>(
                MessageType.JOIN_RESPONSE,
                node.getNodeId(),
                peerEntry.getNodeId(),
                respContent,
                System.currentTimeMillis()
        );
        new MessageSender(node).sendMessage(peerEntry.getIp(), peerEntry.getPort(), resp);

        Message<RoutingEntry> broadcastMsg = new Message<>(
                MessageType.NEW_PEER_BROADCAST,
                node.getNodeId(),
                "ALL",
                peerEntry,
                System.currentTimeMillis()
        );
        node.getRoutingTable().getEntries().stream()
                .filter(e -> !e.getNodeId().equals(peerEntry.getNodeId()))
                .forEach(e -> {
                    new MessageSender(node).sendMessage(e.getIp(), e.getPort(), broadcastMsg);
                });
    }

    private void handleJoinResponse(Message<?> message) {
        JoinResponseContent content = (JoinResponseContent) message.getContent();
        if (content.getRoutingTable() != null) {
            node.getRoutingTable().mergeEntries(Arrays.asList(content.getRoutingTable()));
            System.out.println("[INFO] 라우팅 테이블 병합");
        }

        if (content.getSuperNodeEntry() != null) {
            node.getRoutingTable().setSuperNodeEntry(content.getSuperNodeEntry());
            System.out.println("[INFO] 슈퍼노드 엔트리 갱신");
        }

        if (content.getRedundancyEntry() != null) {
            node.getRoutingTable().setRedundancyEntry(content.getRedundancyEntry());
            System.out.println("[INFO] 레둔던시 엔트리 갱신");
        }
    }

    private void handleNewPeerBroadcast(Message<?> message) {
        RoutingEntry newPeer = (RoutingEntry) message.getContent();
        System.out.println("[INFO] " + newPeer.getNodeId() + " ▶ 새로운 로컬 Peer 등록");
        node.getRoutingTable().addEntry(newPeer);
    }

    private void handlePromoteRedundancy(Message<?> message) {
        node.promoteToRedundancy();
        RoutingEntry selfEntry = new RoutingEntry(node.getNodeId(), node.getIp(), node.getPort(), NodeRole.REDUNDANCY);
        node.getRoutingTable().setRedundancyEntry(selfEntry);
        System.out.println("[INFO] " + node.getNodeId() + " ▶ REDUNDANCY로 승격되었습니다.");
    }

    private void handleNewRedundancyBroadcast(Message<?> message) {
        RoutingEntry redundancyEntry = (RoutingEntry) message.getContent();
        System.out.println("[INFO] " + redundancyEntry.getNodeId() + " ▶ 새로운 Redundancy 등록");
        SuperNodeTable.getInstance().addRedundancy(redundancyEntry);
    }
}
