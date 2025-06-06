package com.p2pnetwork.network;

import com.p2pnetwork.message.Message;
import com.p2pnetwork.message.MessageType;
import com.p2pnetwork.message.dto.AssignSuperNodeContent;
import com.p2pnetwork.message.dto.JoinResponseContent;
import com.p2pnetwork.message.dto.PromotionContent;
import com.p2pnetwork.node.Node;
import com.p2pnetwork.node.NodeRole;
import com.p2pnetwork.routing.RoutingEntry;
import com.p2pnetwork.routing.table.SuperNodeTable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MessageHandler {
    private final Node node;
    private int vote = 0;

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
            case JOIN_REQUEST:
                handleJoinRequest(message);
                break;
            case JOIN_RESPONSE:
                handleJoinResponse(message);
                break;
            case NEW_PEER_BROADCAST:
                handleNewPeerBroadcast(message);
                break;
            case NEW_SUPERNODE_BROADCAST:
                handleNewSuperNodeBroadcast(message);
                break;
            case NEW_REDUNDANCY_BROADCAST:
                handleNewRedundancyBroadcast(message);
                break;
            case PROMOTE_REDUNDANCY:
                handlePromoteRedundancy(message);
                break;
            case REQUEST_TCP_CONNECT:
                handleRequestTCPConnect(message);
                break;
            case REQUEST_TEMP_PROMOTE, REQUEST_PROMOTE:
                handleRequestPromote(message);
                break;
            case ACCEPT_PROMOTE:
                handlePromoteVote(message, true);
                break;
            case DENY_PROMOTE:
                handlePromoteVote(message, false);
                break;
            case BOOTSTRAP_REPLACEMENT:
                handleBootstrapReplacement(message);
                break;
            case BOOTSTRAP_REVIVED:
                handleBootstrapRevived(message);
                break;
            case BOOTSTRAP_WORKING:
                handleBootstrapWorking(message);
                break;
            case PROMOTED_SUPERNODE_BROADCAST:
                handlePromotedSuperNodeBroadcast(message);
                break;
            case PROMOTED_REDUNDANCY_BROADCAST:
                handlePromotedRedundancyBroadcast(message);
                break;
            case SERVERSOCKET_DEAD:
                handleServerSocketDead(message);
                break;
            /*case DEMOTE_PEER:
                handleDemotePeer(message);
                break;
            case SUPERNODE_REVIVED:
                handleSuperNodeRevived(message);
                break;*/
            case CHECK_SUPERNODE:
                handleCheckSuperNode(message);
                break;
            case UPDATE_SUPERNODE_TABLE_SUPER:
                handleUpdateSuperNodeTable(message, false);
                break;
            case UPDATE_SUPERNODE_TABLE_BOOTSTRAP:
                handleUpdateSuperNodeTable(message, true);
                break;
            default:
                break;
        }
    }

    private void handleUpdateSuperNodeTable(Message<?> message, boolean bootstrap) {
        //TODO: 이 메시지가 Redundancy에게 필요할 때 전달될 수 있도록 sendMessaage를 곳곳에 추가하기 --> 완료
        if (bootstrap){
            String geohash5 = (String) message.getContent();
            SuperNodeTable.getInstance().exchangeBootstrap(geohash5);
        }
        else{
            RoutingEntry newEntry = (RoutingEntry) message.getContent();
            if (newEntry.getRole() == NodeRole.SUPERNODE) {
                SuperNodeTable.getInstance().addSuperNode(newEntry);
            }
            else if (newEntry.getRole() == NodeRole.REDUNDANCY){
                SuperNodeTable.getInstance().addRedundancy(newEntry);
            }
        }


    }

    private void handlePromotedRedundancyBroadcast(Message<?> message) {
        //TODO: Table 변경 --> 완료
        RoutingEntry redundancyEntry = (RoutingEntry) message.getContent();

        if (node.hasAtLeastRole(NodeRole.SUPERNODE)){   // SuperNode가 메시지를 받은 경우 SuperNodeTable을 변경
            String geohash5 = redundancyEntry.getNodeId().split("_")[0];
            SuperNodeTable.getInstance().removeRedundancy(geohash5);
            SuperNodeTable.getInstance().addRedundancy(redundancyEntry);
            //TODO: Redundancy에게도 알림 --> 완료
            MessageSender sender = new MessageSender(node);
            sender.sendMessage(node.getRoutingTable().getRedundancyEntry(), new Message<>(
                    MessageType.UPDATE_SUPERNODE_TABLE_SUPER,
                    node.getNodeId(),
                    node.getRoutingTable().getRedundancyEntry().getNodeId(),
                    redundancyEntry,
                    System.currentTimeMillis()
            ));
        }
        else {                                          // Peer가 메시지를 받은 경우 LocalRoutingTable을 변경
            //TODO: Redundancy로 승격되는 노드들 RoutingMap에 Redundancy로 다시 추가
            node.getRoutingTable().removeEntry(redundancyEntry.getNodeId());
            node.getRoutingTable().setRedundancyEntry(redundancyEntry);
            node.getRoutingTable().addEntry(redundancyEntry);
        }
    }

    private void handlePromotedSuperNodeBroadcast(Message<?> message) {
        //TODO: Table 변경 (새로운 SuperNode) --> 완료
        if (node.hasAtLeastRole(NodeRole.SUPERNODE)){   // SuperNode가 메시지를 받은 경우 SuperNodeTable을 변경
            String geohash = (String) message.getContent();
            RoutingEntry newSuperEntry = SuperNodeTable.getInstance().getRedundancyNode(geohash);
            newSuperEntry.setRole(NodeRole.SUPERNODE);
            SuperNodeTable.getInstance().addSuperNode(newSuperEntry);
            //TODO: Redundancy에게도 알림 --> 완료
            MessageSender sender = new MessageSender(node);
            sender.sendMessage(node.getRoutingTable().getRedundancyEntry(), new Message<>(
                    MessageType.UPDATE_SUPERNODE_TABLE_SUPER,
                    node.getNodeId(),
                    node.getRoutingTable().getRedundancyEntry().getNodeId(),
                    newSuperEntry,
                    System.currentTimeMillis()
            ));
        }
        else {                                          // Peer가 메시지를 받은 경우 LocalRoutingTable을 변경
            //TODO: 이전 SuperNode를 RoutingMap에서 없애고, 현재 Redundancy를 SuperNode로 다시 추가 --> 완료
            RoutingEntry newSuperEntry = node.getRoutingTable().getRedundancyEntry();
            newSuperEntry.setRole(NodeRole.SUPERNODE);
            node.getRoutingTable().removeEntry(node.getRoutingTable().getSuperNodeEntry().getNodeId());
            node.getRoutingTable().removeEntry(node.getRoutingTable().getRedundancyEntry().getNodeId());
            node.getRoutingTable().setSuperNodeEntry(newSuperEntry);
            node.getRoutingTable().addEntry(newSuperEntry);
        }
    }

    private void handleBootstrapWorking(Message<?> message) {           // 코드가 BootstrapReplacement와 완전 동일
        //TODO: Table 변경 (Redundancy에서 다시 Bootstrap으로 교체) --> 완료
        String geohash = (String) message.getContent();

        if (node.hasAtLeastRole(NodeRole.SUPERNODE)){   // SuperNode가 메시지를 받은 경우 SuperNodeTable을 변경
            SuperNodeTable.getInstance().exchangeBootstrap(geohash);
            //TODO: Redundancy에게도 알림 --> 완료
            MessageSender sender = new MessageSender(node);
            sender.sendMessage(node.getRoutingTable().getRedundancyEntry(), new Message<>(
                    MessageType.UPDATE_SUPERNODE_TABLE_BOOTSTRAP,
                    node.getNodeId(),
                    node.getRoutingTable().getRedundancyEntry().getNodeId(),
                    geohash,
                    System.currentTimeMillis()
            ));
        }
        else {                                          // Peer가 메시지를 받은 경우 LocalRoutingTable을 변경
            node.getRoutingTable().exchangeBootstrap();
        }
    }

    private void handleBootstrapRevived(Message<?> message) {
        String geohash5 = node.getNodeId().split("_")[0];

        MessageSender sender = new MessageSender(node);
        Message<String> broadcastMsg = new Message<String>(
                MessageType.BOOTSTRAP_WORKING,
                node.getNodeId(),
                "ALL",
                geohash5,
                System.currentTimeMillis()
        );
        SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                .filter(entry -> !entry.getNodeId().equals(node.getNodeId()))           // 자기 자신 제외
                .forEach(entry -> sender.sendMessage(entry.getIp(), entry.getPort(), broadcastMsg));

        node.getRoutingTable().getEntries().stream()
                .filter(entry -> !entry.getNodeId().equals(node.getNodeId()))           // 되살아난 Bootstrap 제외
                .forEach(entry -> sender.sendMessage(entry.getIp(), entry.getPort(), broadcastMsg));

        //TODO: 자신의 SuperNodeTable 변경 (geohash값을 사용해 Bootstrap과 Redundancy를 SuperNodeTable 상 교환) --> 완료
        SuperNodeTable.getInstance().exchangeBootstrap(geohash5);
        node.getRoutingTable().exchangeBootstrap();
    }

    private void handleBootstrapReplacement(Message<?> message) {
        //TODO: Table 변경 (Bootstrap을 임시적으로 Redundancy로 교체) --> 완료
        String geohash = (String) message.getContent();

        if (node.hasAtLeastRole(NodeRole.SUPERNODE)){   // SuperNode가 메시지를 받은 경우 SuperNodeTable을 변경
            SuperNodeTable.getInstance().exchangeBootstrap(geohash);
            //TODO: Redundancy에게도 알림
            MessageSender sender = new MessageSender(node);
            sender.sendMessage(node.getRoutingTable().getRedundancyEntry(), new Message<>(
                    MessageType.UPDATE_SUPERNODE_TABLE_BOOTSTRAP,
                    node.getNodeId(),
                    node.getRoutingTable().getRedundancyEntry().getNodeId(),
                    geohash,
                    System.currentTimeMillis()
            ));
        }
        else {                                          // Peer가 메시지를 받은 경우 LocalRoutingTable을 변경
            node.getRoutingTable().exchangeBootstrap();
        }
    }

    private void handlePromoteVote(Message<?> message, boolean accept) {
        if (!node.hasAtLeastRole(NodeRole.REDUNDANCY)) return;
        RoutingEntry superEntry = node.getRoutingTable().getSuperNodeEntry();
        int superCount = SuperNodeTable.getInstance().getAllSuperNodeEntries().size();

        if (accept){
            boolean end = addVote(superCount);
            if (end) return;
            vote = 0;
        }
        else {
            MessageSender sender = new MessageSender(node);
            sender.sendMessage(
                    superEntry.getIp(),
                    superEntry.getPort(),
                    new Message<>(
                            MessageType.REQUEST_TCP_CONNECT,
                            node.getNodeId(),
                            superEntry.getNodeId(),
                            null,
                            System.currentTimeMillis()
                    )
            );
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            vote = 0;
            return;
        }

        String geohash5 = node.getNodeId().split("_")[0];

        if (superEntry.getRole() == NodeRole.BOOTSTRAP){                // BOOTSTRAP_REPLACEMENT Broadcast

            MessageSender sender = new MessageSender(node);
            Message<String> broadcastMsg = new Message<String>(
                    MessageType.BOOTSTRAP_REPLACEMENT,
                    node.getNodeId(),
                    "ALL",
                    geohash5,
                    System.currentTimeMillis()
            );
            SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                    .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                    .forEach(entry -> sender.sendMessage(entry.getIp(), entry.getPort(), broadcastMsg));

            node.getRoutingTable().getEntries().stream()
                    .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                    .filter(entry -> !entry.getNodeId().equals(node.getNodeId()))           // 자기 자신 제외
                    .forEach(entry -> sender.sendMessage(entry.getIp(), entry.getPort(), broadcastMsg));

            //TODO: 자신의 SuperNodeTable 변경 (geohash값을 사용해 Bootstrap과 Redundancy를 SuperNodeTable 상 교환) --> 완료
            SuperNodeTable.getInstance().exchangeBootstrap(geohash5);
            node.getRoutingTable().exchangeBootstrap();

        }
        else if (superEntry.getRole() == NodeRole.SUPERNODE){           // PROMOTED_SUPERNODE Broadcast

            node.setRole(NodeRole.SUPERNODE);

            MessageSender sender = new MessageSender(node);
            Message<String> broadcastMsg = new Message<String>(
                    MessageType.PROMOTED_SUPERNODE_BROADCAST,
                    node.getNodeId(),
                    "ALL",
                    geohash5,
                    System.currentTimeMillis()
            );
            SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                    .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                    .forEach(entry -> sender.sendMessage(entry.getIp(), entry.getPort(), broadcastMsg));

            node.getRoutingTable().getEntries().stream()
                    .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                    .filter(entry -> !entry.getNodeId().equals(node.getNodeId()))           // 자기 자신 제외
                    .forEach(entry -> sender.sendMessage(entry.getIp(), entry.getPort(), broadcastMsg));

            //TODO: 자신의 SuperNodeTable 및 LocalRoutingTable 변경 (자신을 SuperNode로 지정) --> 완료
            RoutingEntry selfEntry = new RoutingEntry(node.getNodeId(), node.getIp(), node.getPort(), node.getRole());
            SuperNodeTable.getInstance().addSuperNode(selfEntry);

            node.getRoutingTable().removeEntry(superEntry.getNodeId());
            node.getRoutingTable().setSuperNodeEntry(selfEntry);

            Random random = new Random();
            List<RoutingEntry> entriesList = (List<RoutingEntry>) node.getRoutingTable().getEntries();
            RoutingEntry nextRedundancy = null;
            while (true) {
                int randomValue = random.nextInt(node.getRoutingTable().getEntries().size());
                nextRedundancy = entriesList.get(randomValue);
                if (!nextRedundancy.equals(superEntry)){                        // 새 Redundancy는 이전 SuperNode와는 다른 Node
                    if (!checkAlive(nextRedundancy)) continue;                  // 먼저 Node가 살아있는지 확인
                    sender.sendMessage(nextRedundancy, new Message<>(           // TCP 연결 수립
                            MessageType.TCP_CONNECT,
                            node.getNodeId(),
                            nextRedundancy.getNodeId(),
                            null,
                            System.currentTimeMillis()
                    ));

                    PromotionContent content = new PromotionContent(
                            SuperNodeTable.getInstance().getAllSuperNodeEntries().toArray(new RoutingEntry[0]),
                            SuperNodeTable.getInstance().getAllRedundancyNodeEntries().toArray(new RoutingEntry[0])
                    );

                    sender.sendMessage(nextRedundancy, new Message<>(           // Peer를 Redundancy로 승격
                            MessageType.PROMOTE_REDUNDANCY,
                            node.getNodeId(),
                            nextRedundancy.getNodeId(),
                            content,
                            System.currentTimeMillis()
                    ));
                    break;
                }
            }

            //TODO: 자신의 SuperNodeTable 및 LocalRoutingTable 변경 (Redundancy 추가) --> 완료
            nextRedundancy.setRole(NodeRole.REDUNDANCY);

            SuperNodeTable.getInstance().removeRedundancy(geohash5);
            SuperNodeTable.getInstance().addRedundancy(nextRedundancy);

            //TODO: Redundancy를 RoutingMap에서 지웠다 다시 넣음 --> 완료
            node.getRoutingTable().removeEntry(nextRedundancy.getNodeId());
            node.getRoutingTable().setRedundancyEntry(nextRedundancy);
            node.getRoutingTable().addEntry(nextRedundancy);

            Message<RoutingEntry> redundancyBroadcastMsg = new Message<>(   // 새로 Redundancy가 생겼다고 Broadcast
                    MessageType.PROMOTED_REDUNDANCY_BROADCAST,
                    node.getNodeId(),
                    "ALL",
                    nextRedundancy,
                    System.currentTimeMillis()
            );
            SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                    .filter(entry -> !entry.getNodeId().equals(node.getNodeId()))           // 자기 자신 제외
                    .forEach(entry -> sender.sendMessage(entry.getIp(), entry.getPort(), redundancyBroadcastMsg));

            node.getRoutingTable().getEntries().stream()
                    .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                    .filter(entry -> !entry.getNodeId().equals(node.getNodeId()))           // 자기 자신 제외
                    .forEach(entry -> sender.sendMessage(entry.getIp(), entry.getPort(), redundancyBroadcastMsg));

        }
    }

    private synchronized boolean addVote(int superCount) {
        this.vote++;
        return this.vote != superCount;
    }

    private void handleRequestPromote(Message<?> message) {
        if (!node.hasAtLeastRole(NodeRole.SUPERNODE)) return;
        String geohash = (String) message.getContent();
        RoutingEntry superEntry = SuperNodeTable.getInstance().getSuperNode(geohash);
        RoutingEntry redundancyEntry = SuperNodeTable.getInstance().getRedundancyNode(geohash);

        boolean superAlive = checkAlive(superEntry);

        MessageSender sender = new MessageSender(node);
        if (superAlive) {           // SuperNode가 살아있다면 승격 불허
            sender.sendMessage(
                    redundancyEntry.getIp(),
                    redundancyEntry.getPort(),
                    new Message<>(
                            MessageType.DENY_PROMOTE,
                            node.getNodeId(),
                            redundancyEntry.getNodeId(),
                            null,
                            System.currentTimeMillis()
                    )
            );
        }
        else {                      // SuperNode가 죽어있다면 승격 허가
            sender.sendMessage(
                    redundancyEntry.getIp(),
                    redundancyEntry.getPort(),
                    new Message<>(
                            MessageType.ACCEPT_PROMOTE,
                            node.getNodeId(),
                            redundancyEntry.getNodeId(),
                            null,
                            System.currentTimeMillis()
                    )
            );
        }
    }

    private boolean checkAlive(RoutingEntry nodeEntry) {
        Socket socket = new Socket();
        try {
            SocketAddress address = new InetSocketAddress(nodeEntry.getIp(), nodeEntry.getPort());
            socket.connect(address, 1000);
            return true;
        } catch (Exception e){
            System.out.println("[INFO] " + nodeEntry.getNodeId() + " 죽음 확인");
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("[ERROR] Socket Closing Error");
            }
        }
    }

    private void handleServerSocketDead(Message<?> message) {
        if (!node.hasAtLeastRole(NodeRole.REDUNDANCY)) return;
        String geohash5 = node.getNodeId().split("_")[0];
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
        else if (superEntry.getRole() == NodeRole.SUPERNODE){
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

    private void handleCheckSuperNode(Message<?> message) {
        //TODO: Redundancy가 SuperNode가 살아있는지 확인하고 죽었다면 승격 요청 시작
        if (!node.hasAtLeastRole(NodeRole.REDUNDANCY)) return;
        boolean superAlive = checkAlive(node.getRoutingTable().getSuperNodeEntry());

        if (!superAlive){
            //TODO: handleServerSocketDead와 동일한 작업 --> 메소드 따로 만들기
        }
    }

    private void handleRequestTCPConnect(Message<?> message) {
        //TODO: TCP 연결 재수립
        RoutingEntry redundancyEntry = SuperNodeTable.getInstance().getRedundancyNode(node.getNodeId().split("_")[0]);
        MessageSender sender = new MessageSender(node);
        sender.sendMessage(redundancyEntry, new Message<>(           // TCP 연결 수립
                MessageType.TCP_CONNECT,
                node.getNodeId(),
                redundancyEntry.getNodeId(),
                null,
                System.currentTimeMillis()
        ));
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
            peerEntry.setRole(NodeRole.SUPERNODE);
            SuperNodeTable.getInstance().addSuperNode(peerEntry);
            AssignSuperNodeContent content = new AssignSuperNodeContent(
                    true,
                    peerEntry,
                    SuperNodeTable.getInstance().getAllSuperNodeEntries().toArray(new RoutingEntry[0]),
                    SuperNodeTable.getInstance().getAllRedundancyNodeEntries().toArray(new RoutingEntry[0])
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
                    .filter(entry -> !entry.getNodeId().equals(peerEntry.getNodeId())
                            && !entry.getNodeId().equals(node.getNodeId()))
                    .forEach(entry -> sender.sendMessage(entry.getIp(), entry.getPort(), broadcastMsg));
        } else {
            // 기존 슈퍼노드 할당
            AssignSuperNodeContent content = new AssignSuperNodeContent(
                    false,
                    existing,
                    null,
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
            if (content.getSuperNodes() != null) {
                Arrays.stream(content.getSuperNodes())
                        .forEach(SuperNodeTable.getInstance()::addSuperNode);
                System.out.println("[INFO] 슈퍼노드 테이블 갱신");
            }
            if (content.getRedundancies() != null) {
                Arrays.stream(content.getRedundancies())
                        .forEach(SuperNodeTable.getInstance()::addRedundancy);
                System.out.println("[INFO] 레둔던시 테이블 갱신");
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


    private void handleNewRedundancyBroadcast(Message<?> message) {
        RoutingEntry redundancyEntry = (RoutingEntry) message.getContent();
        System.out.println("[INFO] " + redundancyEntry.getNodeId() + " ▶ 새로운 Redundancy 등록");
        SuperNodeTable.getInstance().addRedundancy(redundancyEntry);

        String geohash5 = node.getNodeId().split("_")[0];
        if (geohash5.equals(redundancyEntry.getNodeId().split("_")[0])) {
            MessageSender sender = new MessageSender(node);
            sender.sendMessage(redundancyEntry, new Message<>(           // TCP 연결 수립
                    MessageType.TCP_CONNECT,
                    node.getNodeId(),
                    redundancyEntry.getNodeId(),
                    null,
                    System.currentTimeMillis()
            ));
            SuperNodeTable.getInstance().showSuperNodeEntries();
        }
    }

    private void handleJoinRequest(Message<?> message) {
        RoutingEntry peerEntry = (RoutingEntry) message.getContent();
        node.getRoutingTable().addEntry(peerEntry);

        boolean isFirstPeer = node.getRoutingTable().getEntries().size() == 2; // 본인+peer

        if (isFirstPeer) {
            peerEntry.setRole(NodeRole.REDUNDANCY);
            node.getRoutingTable().setRedundancyEntry(peerEntry);
            node.getRoutingTable().addEntry(peerEntry);
            SuperNodeTable.getInstance().addRedundancy(peerEntry);

            PromotionContent content = new PromotionContent(
                    SuperNodeTable.getInstance().getAllSuperNodeEntries().toArray(new RoutingEntry[0]),
                    SuperNodeTable.getInstance().getAllRedundancyNodeEntries().toArray(new RoutingEntry[0])
            );

            Message<PromotionContent> promoteMsg = new Message<>(
                    MessageType.PROMOTE_REDUNDANCY,
                    node.getNodeId(),
                    peerEntry.getNodeId(),
                    content,
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
            SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                    .filter(entry -> !entry.getNodeId().equals(node.getNodeId()))
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
                .filter(entry -> !entry.getNodeId().equals(peerEntry.getNodeId())
                        && !entry.getNodeId().equals(node.getNodeId()))
                .forEach(entry -> {
                    new MessageSender(node).sendMessage(entry.getIp(), entry.getPort(), broadcastMsg);
                });
    }

    private void handleJoinResponse(Message<?> message) {
        JoinResponseContent content = (JoinResponseContent) message.getContent();
        if (content.getRoutingTable() != null) {
            node.getRoutingTable().replaceEntries(Arrays.asList(content.getRoutingTable()));
            System.out.println("[INFO] 라우팅 테이블 교체 완료");
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
        PromotionContent content = (PromotionContent) message.getContent();

        SuperNodeTable superNodeTable = SuperNodeTable.getInstance();
        superNodeTable.clear();

        if (content.getSuperNodes() != null) {
            for (RoutingEntry entry : content.getSuperNodes()) {
                superNodeTable.addSuperNode(entry);
            }
        }
        if (content.getRedundancies() != null) {
            for (RoutingEntry entry : content.getRedundancies()) {
                superNodeTable.addRedundancy(entry);
            }
        }
        node.promoteToRedundancy();
        superNodeTable.printTable();
        node.getRoutingTable().printTable();
        System.out.println("[INFO] " + node.getNodeId() + " ▶ REDUNDANCY로 승격 및 테이블 동기화 완료");
    }

    /*private void handleSuperNodeRevived(Message<?> message) {           // message에 이전 SuperNode의 RoutingEntry가 들어가 있음
        RoutingEntry oldSuperEntry = (RoutingEntry) message.getContent();
        MessageSender sender = new MessageSender(node);

        if (!node.hasAtLeastRole(NodeRole.SUPERNODE)){                  // Peer가 받았을 경우 현 SuperNode에게 재전달
            RoutingEntry currentSuperEntry = node.getRoutingTable().getSuperNodeEntry();
            sender.sendMessage(currentSuperEntry, new Message<>(
                    MessageType.SUPERNODE_REVIVED,
                    node.getNodeId(),
                    currentSuperEntry.getNodeId(),
                    oldSuperEntry,
                    System.currentTimeMillis()
            ));
        }
        else {                                                          // SuperNode가 받았을 경우
            sender.sendMessage(oldSuperEntry, new Message<>(
                    MessageType.DEMOTE_PEER,
                    node.getNodeId(),
                    oldSuperEntry.getNodeId(),
                    null,
                    System.currentTimeMillis()
            ));
        }
    }

    private void handleDemotePeer(Message<?> message) {
        node.setRole(NodeRole.PEER);
        //TODO: SuperNodeTable 리셋 (?)
    }*/
}
