package com.p2pnetwork.network;

import com.p2pnetwork.message.Message;
import com.p2pnetwork.message.MessageType;
import com.p2pnetwork.message.dto.*;
import com.p2pnetwork.node.Node;
import com.p2pnetwork.node.NodeRole;
import com.p2pnetwork.routing.RoutingEntry;
import com.p2pnetwork.routing.file.FileMetadataTable;
import com.p2pnetwork.routing.table.SuperNodeTable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageHandler {
    private final Node node;
    private int vote = 0;
    private final AtomicInteger superCount = new AtomicInteger(0);
    public void setSuperCount(int superCount) {
        this.superCount.set(superCount);
    }

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
            case BOOTSTRAP_TABLE_SYNC:
                handleBootstrapTableSync(message);
                break;
            case BOOTSTRAP_WORKING:
                handleBootstrapWorking(message);
                break;
            case REDUNDANCY_REVIVED:
                handleRedundancyRevived(message);
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
                break;
            case CHECK_SUPERNODE:
                handleCheckSuperNode(message);
                break;*/
            case UPDATE_SUPERNODE_TABLE_SUPER:
                handleUpdateSuperNodeTable(message, false);
                break;
            case UPDATE_SUPERNODE_TABLE_BOOTSTRAP:
                handleUpdateSuperNodeTable(message, true);
                break;
            case REDUNDANCY_DISCONNECT:
                handleRedundancyDisconnect(message);
                break;
            case REMOVE_REDUNDANCY_BROADCAST:
                handleRemoveRedundancyBroadcast(message);
                break;
            case REQUEST_FILE_METADATA:
                handleRequestFileMetadata(message);
                break;
            case REQUEST_FILE_DOWNLOAD:
                handleRequestFileDownload(message);
                break;
            case RESPOND_FILE_METADATA:
                handleResponseFileMetadata(message);
                break;
            case RESPOND_FILE_DOWNLOAD:
                handleResponseFileDownload(message);
                break;
            case NOTIFY_FILE_DOWNLOAD:
                handleNotifyFileDownload(message);
                break;
            case SYNC_FILE_METADATA:
                handleSyncFileMetadata(message);
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

        //System.out.println("Log - Update SuperNode Table Handler: Table Update Successful");
        //SuperNodeTable.getInstance().printTable();

    }

    private void handlePromotedRedundancyBroadcast(Message<?> message) {
        //TODO: Table 변경 --> 완료
        RoutingEntry oldRedundancyEntry = node.getRoutingTable().getRedundancyEntry();
        RoutingEntry redundancyEntry = (RoutingEntry) message.getContent();

        if (node.hasAtLeastRole(NodeRole.REDUNDANCY)){   // SuperNode가 메시지를 받은 경우 SuperNodeTable을 변경

            //System.out.println("Log - Promoted Redundancy Broadcast Handler: SuperNode Case");

            String geohash5 = redundancyEntry.getNodeId().split("_")[0];
            SuperNodeTable.getInstance().removeRedundancy(geohash5);
            SuperNodeTable.getInstance().addRedundancy(redundancyEntry);

            //System.out.println("Log - Promoted Redundancy Broadcast Handler: Table Update Complete");
            //SuperNodeTable.getInstance().printTable();

            //TODO: Redundancy에게도 알림 --> 완료
            MessageSender sender = new MessageSender(node);
            sender.sendMessage(node.getRoutingTable().getRedundancyEntry(), new Message<>(
                    MessageType.UPDATE_SUPERNODE_TABLE_SUPER,
                    node.getNodeId(),
                    node.getRoutingTable().getRedundancyEntry().getNodeId(),
                    redundancyEntry,
                    System.currentTimeMillis()
            ));

            //System.out.println("Log - Promoted SuperNode Broadcast Handler: Sent Info To Redundancy");

        }
        else {                                          // Peer가 메시지를 받은 경우 LocalRoutingTable을 변경
            //TODO: Redundancy로 승격되는 노드들 RoutingMap에 Redundancy로 다시 추가

            //System.out.println("Log - Promoted Redundancy Broadcast Handler: Peer Case");

            node.getRoutingTable().removeEntry(redundancyEntry.getNodeId());
            node.getRoutingTable().setRedundancyEntry(redundancyEntry);
            node.getRoutingTable().addEntry(redundancyEntry);

            if (!redundancyEntry.getNodeId().equals(node.getNodeId())){
                node.getRoutingTable().removeEntry(oldRedundancyEntry.getNodeId());
                oldRedundancyEntry.setRole(NodeRole.PEER);
                node.getRoutingTable().addEntry(oldRedundancyEntry);
            }

            //System.out.println("Log - Promoted Redundancy Broadcast Handler: Table Update Complete");
            //node.getRoutingTable().printTable();

        }
    }

    private void handlePromotedSuperNodeBroadcast(Message<?> message) {
        //TODO: Table 변경 (새로운 SuperNode) --> 완료
        if (node.hasAtLeastRole(NodeRole.REDUNDANCY)){   // SuperNode가 메시지를 받은 경우 SuperNodeTable을 변경

            //System.out.println("Log - Promoted SuperNode Broadcast Handler: SuperNode Case");

            String geohash = (String) message.getContent();
            //System.out.println("Log - Promoted SuperNode Broadcast Handler: geohash: " + geohash);

            RoutingEntry newSuperEntry = SuperNodeTable.getInstance().getRedundancyNode(geohash);
            newSuperEntry.setRole(NodeRole.SUPERNODE);
            SuperNodeTable.getInstance().addSuperNode(newSuperEntry);

            //System.out.println("Log - Promoted SuperNode Broadcast Handler: Table Update Complete");
            //SuperNodeTable.getInstance().printTable();

            //TODO: Redundancy에게도 알림 --> 완료
            MessageSender sender = new MessageSender(node);
            sender.sendMessage(node.getRoutingTable().getRedundancyEntry(), new Message<>(
                    MessageType.UPDATE_SUPERNODE_TABLE_SUPER,
                    node.getNodeId(),
                    node.getRoutingTable().getRedundancyEntry().getNodeId(),
                    newSuperEntry,
                    System.currentTimeMillis()
            ));

            //System.out.println("Log - Promoted SuperNode Broadcast Handler: Sent Info To Redundancy");

        }
        else {                                          // Peer가 메시지를 받은 경우 LocalRoutingTable을 변경
            //TODO: 이전 SuperNode를 RoutingMap에서 없애고, 현재 Redundancy를 SuperNode로 다시 추가 --> 완료

            //System.out.println("Log - Promoted SuperNode Broadcast Handler: Peer Case");

            RoutingEntry newSuperEntry = node.getRoutingTable().getRedundancyEntry();
            newSuperEntry.setRole(NodeRole.SUPERNODE);
            node.getRoutingTable().removeEntry(node.getRoutingTable().getSuperNodeEntry().getNodeId());
            node.getRoutingTable().removeEntry(node.getRoutingTable().getRedundancyEntry().getNodeId());
            node.getRoutingTable().setSuperNodeEntry(newSuperEntry);
            node.getRoutingTable().addEntry(newSuperEntry);

            //System.out.println("Log - Promoted SuperNode Broadcast Handler: Table Update Complete");
            //node.getRoutingTable().printTable();


        }
    }

    private void handleBootstrapWorking(Message<?> message) {           // 코드가 BootstrapReplacement와 완전 동일
        //TODO: Table 변경 (Redundancy에서 다시 Bootstrap으로 교체) --> 완료
        String geohash = (String) message.getContent();

        if (node.hasAtLeastRole(NodeRole.REDUNDANCY)){   // SuperNode가 메시지를 받은 경우 SuperNodeTable을 변경
            SuperNodeTable.getInstance().exchangeBootstrap(geohash);

            //System.out.println("Log - Bootstrap Working Handler: Table Update Complete");
            //SuperNodeTable.getInstance().printTable();

            //TODO: Redundancy에게도 알림 --> 완료
            MessageSender sender = new MessageSender(node);
            sender.sendMessage(node.getRoutingTable().getRedundancyEntry(), new Message<>(
                    MessageType.UPDATE_SUPERNODE_TABLE_BOOTSTRAP,
                    node.getNodeId(),
                    node.getRoutingTable().getRedundancyEntry().getNodeId(),
                    geohash,
                    System.currentTimeMillis()
            ));

            //System.out.println("Log - Bootstrap Working Handler: Sent Info To Redundancy");

        }
        else {                                          // Peer가 메시지를 받은 경우 LocalRoutingTable을 변경
            node.getRoutingTable().exchangeBootstrap();

            //System.out.println("Log - Bootstrap Working Handler: Table Update Complete");
            //node.getRoutingTable().printTable();
        }
    }

    private void handleBootstrapRevived(Message<?> message) {
        String geohash5 = node.getNodeId().split("_")[0];

        SuperNodeTable.getInstance().exchangeBootstrap(geohash5);
        node.getRoutingTable().exchangeBootstrap();

        MessageSender sender = new MessageSender(node);
        RoutingEntry bootstrapEntry = SuperNodeTable.getInstance().getSuperNode(geohash5);
        SyncAllTableContent content = new SyncAllTableContent(
                SuperNodeTable.getInstance().getAllSuperNodeEntries().toArray(new RoutingEntry[0]),
                SuperNodeTable.getInstance().getAllRedundancyNodeEntries().toArray(new RoutingEntry[0]),

                node.getRoutingTable().getSuperNodeEntry(),
                node.getRoutingTable().getRedundancyEntry(),
                node.getRoutingTable().getEntries().toArray(new RoutingEntry[0]),

                FileMetadataTable.getInstance().getMetadataMap()
        );

        sender.sendMessage(bootstrapEntry, new Message<>(
                MessageType.BOOTSTRAP_TABLE_SYNC,
                node.getNodeId(),
                bootstrapEntry.getNodeId(),
                content,
                System.currentTimeMillis()
        ));

        Message<String> broadcastMsg = new Message<String>(
                MessageType.BOOTSTRAP_WORKING,
                node.getNodeId(),
                "ALL",
                geohash5,
                System.currentTimeMillis()
        );

        SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                .filter(entry -> !entry.getNodeId().equals(bootstrapEntry.getNodeId()))           // 되살아난 Bootstrap 제외
                .forEach(entry -> sender.sendMessage(entry, broadcastMsg));

        node.getRoutingTable().getEntries().stream()
                .filter(entry -> !entry.getNodeId().equals(node.getRoutingTable().getSuperNodeEntry().getNodeId()))     // 되살아난 Bootstrap 제외
                .filter(entry -> !entry.getNodeId().equals(node.getNodeId()))                                           // 자기 자신 제외
                .forEach(entry -> sender.sendMessage(entry, broadcastMsg));

        //TODO: 자신의 SuperNodeTable 변경 (geohash값을 사용해 Bootstrap과 Redundancy를 SuperNodeTable 상 교환) --> 완료
    }

    private void handleRedundancyRevived(Message<?> message) {
        String geohash5 = node.getNodeId().split("_")[0];

        MessageSender sender = new MessageSender(node);
        RoutingEntry redundancyEntry = SuperNodeTable.getInstance().getRedundancyNode(geohash5);
        SyncAllTableContent content = new SyncAllTableContent(
                SuperNodeTable.getInstance().getAllSuperNodeEntries().toArray(new RoutingEntry[0]),
                SuperNodeTable.getInstance().getAllRedundancyNodeEntries().toArray(new RoutingEntry[0]),

                node.getRoutingTable().getSuperNodeEntry(),
                node.getRoutingTable().getRedundancyEntry(),
                node.getRoutingTable().getEntries().toArray(new RoutingEntry[0]),

                FileMetadataTable.getInstance().getMetadataMap()
        );

        sender.sendMessage(redundancyEntry, new Message<>(
                MessageType.BOOTSTRAP_TABLE_SYNC,
                node.getNodeId(),
                redundancyEntry.getNodeId(),
                content,
                System.currentTimeMillis()
        ));
    }

    private void handleBootstrapTableSync(Message<?> message) {
        SyncAllTableContent content = (SyncAllTableContent) message.getContent();

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

        if (content.getMetadataMap() != null) {
            FileMetadataTable.getInstance().setMetadataMap(content.getMetadataMap());
            System.out.println("[INFO] 파일 메타데이터 테이블 갱신");
        }

        //SuperNodeTable.getInstance().printTable();
        //node.getRoutingTable().printTable();
        //FileMetadataTable.getInstance().printTable();
    }

    private void handleBootstrapReplacement(Message<?> message) {
        //TODO: Table 변경 (Bootstrap을 임시적으로 Redundancy로 교체) --> 완료
        String geohash = (String) message.getContent();

        if (node.hasAtLeastRole(NodeRole.REDUNDANCY)){   // SuperNode가 메시지를 받은 경우 SuperNodeTable을 변경
            SuperNodeTable.getInstance().exchangeBootstrap(geohash);

            //System.out.println("Log - Bootstrap Working Handler: Table Update Complete");
            //SuperNodeTable.getInstance().printTable();

            //TODO: Redundancy에게도 알림
            MessageSender sender = new MessageSender(node);
            sender.sendMessage(node.getRoutingTable().getRedundancyEntry(), new Message<>(
                    MessageType.UPDATE_SUPERNODE_TABLE_BOOTSTRAP,
                    node.getNodeId(),
                    node.getRoutingTable().getRedundancyEntry().getNodeId(),
                    geohash,
                    System.currentTimeMillis()
            ));

            //System.out.println("Log - Bootstrap Working Handler: Sent Info To Redundancy");

        }
        else {                                          // Peer가 메시지를 받은 경우 LocalRoutingTable을 변경
            node.getRoutingTable().exchangeBootstrap();

            //System.out.println("Log - Bootstrap Working Handler: Table Update Complete");
            //node.getRoutingTable().printTable();
        }
    }

    private void handlePromoteVote(Message<?> message, boolean accept) {
        if (!node.hasAtLeastRole(NodeRole.REDUNDANCY)) return;
        RoutingEntry superEntry = node.getRoutingTable().getSuperNodeEntry();

        if (accept){
            boolean end = addVote();
            if (end) return;
            vote = 0;
        }
        else {
            //TODO: SuperNode 죽음 기다렸다 재확인, 죽었으면 다시 승격 요청, 살아 있으면 Bootstrap일 땐 재연결, SuperNode일 땐 자기 자신 강등 --> 완료
            try {
            Thread.sleep(10000);

            if (this.superCount.get() != -1) {
                this.superCount.set(-1);
            }
            else return;

            vote = 0;
            if (checkAlive(superEntry)) {
                if (superEntry.getRole() == NodeRole.SUPERNODE) {
                    //SuperNodeTable superNodeTable = SuperNodeTable.getInstance();
                    //superNodeTable.clear();
                    node.setRole(NodeRole.PEER);
                }
                else if (superEntry.getRole() == NodeRole.BOOTSTRAP) {
                    new MessageSender(node).sendMessage(superEntry, new Message<>(
                            MessageType.REQUEST_TCP_CONNECT,
                            node.getNodeId(),
                            superEntry.getNodeId(),
                            null,
                            System.currentTimeMillis()
                    ));
                }
                return;
            }

            this.superCount.set(0);
            for (RoutingEntry entry : SuperNodeTable.getInstance().getAllSuperNodeEntries()){
                if (entry.getRole() == NodeRole.SUPERNODE) continue;
                new Thread(() -> {
                    Socket socket = new Socket();
                    try {
                        SocketAddress address = new InetSocketAddress(entry.getIp(), entry.getPort());
                        socket.connect(address, 1000);
                        this.superCount.getAndIncrement();
                    } catch (Exception ex){
                        //System.out.println("[INFO] " + entry.getNodeId() + " 죽음 확인");
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
            String geohash5 = node.getNodeId().split("_")[0];

            if (superEntry.getRole() == NodeRole.BOOTSTRAP) {
                Message<String> broadcastMsg = new Message<String>(
                        MessageType.REQUEST_TEMP_PROMOTE,
                        node.getNodeId(),
                        "Bootstrap",
                        geohash5,
                        System.currentTimeMillis()
                );
                SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                        .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                        .filter(entry -> !entry.getRole().equals(NodeRole.SUPERNODE))
                        .forEach(entry -> sender.sendMessage(entry, broadcastMsg));
            }
            else if (superEntry.getRole() == NodeRole.SUPERNODE){
                Message<String> broadcastMsg = new Message<String>(
                        MessageType.REQUEST_PROMOTE,
                        node.getNodeId(),
                        "Bootstrap",
                        geohash5,
                        System.currentTimeMillis()
                );
                SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                        .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                        .filter(entry -> !entry.getRole().equals(NodeRole.SUPERNODE))
                        .forEach(entry -> sender.sendMessage(entry, broadcastMsg));
            }

            Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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
                    .forEach(entry -> sender.sendMessage(entry, broadcastMsg));

            node.getRoutingTable().getEntries().stream()
                    .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                    .filter(entry -> !entry.getNodeId().equals(node.getNodeId()))           // 자기 자신 제외
                    .forEach(entry -> sender.sendMessage(entry, broadcastMsg));

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
                    .forEach(entry -> sender.sendMessage(entry, broadcastMsg));

            node.getRoutingTable().getEntries().stream()
                    .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                    .filter(entry -> !entry.getNodeId().equals(node.getNodeId()))           // 자기 자신 제외
                    .forEach(entry -> sender.sendMessage(entry, broadcastMsg));

            //System.out.println("Log - Promote Vote Handler: Promoted SuperNode Broadcast Done");

            //TODO: 자신의 SuperNodeTable 및 LocalRoutingTable 변경 (자신을 SuperNode로 지정) --> 완료
            RoutingEntry selfEntry = new RoutingEntry(node.getNodeId(), node.getIp(), node.getPort(), node.getRole());
            SuperNodeTable.getInstance().addSuperNode(selfEntry);

            node.getRoutingTable().removeEntry(superEntry.getNodeId());
            node.getRoutingTable().setSuperNodeEntry(selfEntry);

            //System.out.println("Log - Promote Vote Handler: Table Update Complete");
            //node.getRoutingTable().printTable();

            Random random = new Random();
            List<RoutingEntry> entriesList = new ArrayList<>(node.getRoutingTable().getEntries());
            HashSet<Integer> checkedEntries = new HashSet<>();
            RoutingEntry nextRedundancy = null;
            while (true) {
                //TODO: 살아있는 노드가 없을 때 처리해줘야 함 --> 완료
                if (checkedEntries.size() == entriesList.size()) {
                    System.out.println("[INFO] 레둔던시로 전환할 노드가 존재하지 않음");
                    nextRedundancy = null;
                    break;
                }
                int randomValue = random.nextInt(entriesList.size());
                if (checkedEntries.contains(randomValue)) continue;
                checkedEntries.add(randomValue);

                nextRedundancy = entriesList.get(randomValue);
                if (!nextRedundancy.equals(superEntry) && !nextRedundancy.equals(selfEntry)){
                    if (!checkAlive(nextRedundancy)) continue;                  // 먼저 Node가 살아있는지 확인

                    //System.out.println("Log - Promote Vote Handler: Next Redundancy Node Decided");

                    sender.sendMessage(nextRedundancy, new Message<>(           // TCP 연결 수립
                            MessageType.TCP_CONNECT,
                            node.getNodeId(),
                            nextRedundancy.getNodeId(),
                            null,
                            System.currentTimeMillis()
                    ));

                    //System.out.println("Log - Promote Vote Handler: TCP_CONNECT Sent");

                    PromotionContent content = new PromotionContent(
                            SuperNodeTable.getInstance().getAllSuperNodeEntries().toArray(new RoutingEntry[0]),
                            SuperNodeTable.getInstance().getAllRedundancyNodeEntries().toArray(new RoutingEntry[0]),
                            FileMetadataTable.getInstance().getMetadataMap()
                    );

                    sender.sendMessage(nextRedundancy, new Message<>(           // Peer를 Redundancy로 승격
                            MessageType.PROMOTE_REDUNDANCY,
                            node.getNodeId(),
                            nextRedundancy.getNodeId(),
                            content,
                            System.currentTimeMillis()
                    ));

                    //System.out.println("Log - Promote Vote Handler: TCP_CONNECT Sent");

                    break;
                }
            }

            if (nextRedundancy == null) {
                Message<String> removeRedundancyBroadcastMsg = new Message<>(
                        MessageType.REMOVE_REDUNDANCY_BROADCAST,
                        node.getNodeId(),
                        "Super Node",
                        geohash5,
                        System.currentTimeMillis()
                );

                SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                        .filter(entry -> !entry.getNodeId().equals(node.getNodeId()))           // 자기 자신 제외
                        .forEach(entry -> sender.sendMessage(entry, removeRedundancyBroadcastMsg));

                node.getRoutingTable().removeEntry(superEntry.getNodeId());
                superEntry.setRole(NodeRole.PEER);
                node.getRoutingTable().addEntry(superEntry);
                node.getRoutingTable().setRedundancyEntry(null);

                return;
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
                    .forEach(entry -> sender.sendMessage(entry, redundancyBroadcastMsg));

            node.getRoutingTable().getEntries().stream()
                    .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                    .filter(entry -> !entry.getNodeId().equals(node.getNodeId()))           // 자기 자신 제외
                    .forEach(entry -> sender.sendMessage(entry, redundancyBroadcastMsg));

            //System.out.println("Log - Promote Vote Handler: Promoted Redundancy Broadcast Done");

        }
    }

    private synchronized boolean addVote() {
        this.vote++;
        return this.vote != this.superCount.get();                 // TODO: -3는 안 켜진 Bootstrap을 위한 것이기 때문에 나중에 지움
    }

    private void handleRequestPromote(Message<?> message) {
        if (!node.hasAtLeastRole(NodeRole.REDUNDANCY)) return;
        //System.out.println("Log - Request Promote Handler: Begin");

        String geohash = (String) message.getContent();
        RoutingEntry superEntry = SuperNodeTable.getInstance().getSuperNode(geohash);
        RoutingEntry redundancyEntry = SuperNodeTable.getInstance().getRedundancyNode(geohash);

        //System.out.println("Log - Request Promote Handler: Fields Calculated, Begin Check Alive");

        boolean superAlive = checkAlive(superEntry);

        //System.out.println("Log - Request Promote: Check Alive Successful");

        MessageSender sender = new MessageSender(node);
        if (superAlive) {           // SuperNode가 살아있다면 승격 불허

            //System.out.println("Log - Request Promote: Deny Promote");

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

            //System.out.println("Log - Request Promote: Accept Promote");

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

        this.superCount.set(0);
        for (RoutingEntry entry : SuperNodeTable.getInstance().getAllSuperNodeEntries()){
            if (entry.getRole() == NodeRole.SUPERNODE) continue;
            new Thread(() -> {
                Socket socket = new Socket();
                try {
                    SocketAddress address = new InetSocketAddress(entry.getIp(), entry.getPort());
                    socket.connect(address, 1000);
                    this.superCount.getAndIncrement();
                } catch (Exception ex){
                    //System.out.println("[INFO] " + entry.getNodeId() + " 죽음 확인");
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
                    "Bootstrap",
                    geohash5,
                    System.currentTimeMillis()
            );
            SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                    .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                    .filter(entry -> !entry.getRole().equals(NodeRole.SUPERNODE))
                    .forEach(entry -> sender.sendMessage(entry, broadcastMsg));
        }
        else if (superEntry.getRole() == NodeRole.SUPERNODE){
            Message<String> broadcastMsg = new Message<String>(
                    MessageType.REQUEST_PROMOTE,
                    node.getNodeId(),
                    "Bootstrap",
                    geohash5,
                    System.currentTimeMillis()
            );
            SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                    .filter(entry -> !entry.getNodeId().equals(superEntry.getNodeId()))     // 죽은 SuperNode 제외
                    .filter(entry -> !entry.getRole().equals(NodeRole.SUPERNODE))
                    .forEach(entry -> sender.sendMessage(entry, broadcastMsg));
        }
    }

    /*private void handleCheckSuperNode(Message<?> message) {
        //TODO: Redundancy가 SuperNode가 살아있는지 확인하고 죽었다면 승격 요청 시작
        if (!node.hasAtLeastRole(NodeRole.REDUNDANCY)) return;
        boolean superAlive = checkAlive(node.getRoutingTable().getSuperNodeEntry());

        if (!superAlive){
            //TODO: handleServerSocketDead와 동일한 작업 --> 메소드 따로 만들기
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
    }*/

    private void handleRequestTCPConnect(Message<?> message) {
        //TODO: TCP 연결 재수립 --> 완료
        RoutingEntry redundancyEntry = SuperNodeTable.getInstance().getRedundancyNode(node.getNodeId().split("_")[0]);
        MessageSender sender = new MessageSender(node);
        sender.sendMessage(redundancyEntry, new Message<>(              // TCP 연결 수립
                MessageType.TCP_CONNECT,
                node.getNodeId(),
                redundancyEntry.getNodeId(),
                null,
                System.currentTimeMillis()
        ));
    }

    private void handleRedundancyDisconnect(Message<?> message) {
        if (node.hasAtLeastRole(NodeRole.BOOTSTRAP)) {
            System.out.println("[INFO] Bootstrap이 Redundancy와 연결이 끊어졌습니다. Redundancy의 복구를 기다립니다.");
            return;
        }

        String geohash5 = node.getNodeId().split("_")[0];
        RoutingEntry redundancyEntry = SuperNodeTable.getInstance().getRedundancyNode(geohash5);
        RoutingEntry selfEntry = new RoutingEntry(node.getNodeId(), node.getIp(), node.getPort(), node.getRole());
        MessageSender sender = new MessageSender(node);

        if (checkAlive(redundancyEntry)) {
            sender.sendMessage(redundancyEntry, new Message<>(          // TCP 연결 수립
                    MessageType.TCP_CONNECT,
                    node.getNodeId(),
                    redundancyEntry.getNodeId(),
                    null,
                    System.currentTimeMillis()
            ));
        }
        else {

            Random random = new Random();
            List<RoutingEntry> entriesList = new ArrayList<>(node.getRoutingTable().getEntries());
            HashSet<Integer> checkedEntries = new HashSet<>();
            RoutingEntry nextRedundancy = null;
            while (true) {
                //TODO: 살아있는 노드가 없을 때 처리해줘야 함 --> 완료
                if (checkedEntries.size() == entriesList.size()) {
                    System.out.println("[INFO] 레둔던시로 전환할 노드가 존재하지 않음");
                    nextRedundancy = null;
                    break;
                }
                int randomValue = random.nextInt(entriesList.size());
                if (checkedEntries.contains(randomValue)) continue;
                checkedEntries.add(randomValue);

                nextRedundancy = entriesList.get(randomValue);
                if (!nextRedundancy.equals(redundancyEntry) && !nextRedundancy.equals(selfEntry) && nextRedundancy.getRole().equals(NodeRole.PEER)){
                    if (!checkAlive(nextRedundancy)) continue;                  // 먼저 Node가 살아있는지 확인

                    //System.out.println("Log - Redundancy Disconnect Handler: Next Redundancy Node Decided");

                    sender.sendMessage(nextRedundancy, new Message<>(           // TCP 연결 수립
                            MessageType.TCP_CONNECT,
                            node.getNodeId(),
                            nextRedundancy.getNodeId(),
                            null,
                            System.currentTimeMillis()
                    ));

                    //System.out.println("Log - Redundancy Disconnect Handler: TCP_CONNECT Sent");

                    PromotionContent content = new PromotionContent(
                            SuperNodeTable.getInstance().getAllSuperNodeEntries().toArray(new RoutingEntry[0]),
                            SuperNodeTable.getInstance().getAllRedundancyNodeEntries().toArray(new RoutingEntry[0]),
                            FileMetadataTable.getInstance().getMetadataMap()
                    );

                    sender.sendMessage(nextRedundancy, new Message<>(           // Peer를 Redundancy로 승격
                            MessageType.PROMOTE_REDUNDANCY,
                            node.getNodeId(),
                            nextRedundancy.getNodeId(),
                            content,
                            System.currentTimeMillis()
                    ));

                    //System.out.println("Log - Redundancy Disconnect Handler: TCP_CONNECT Sent");

                    break;
                }
            }

            if (nextRedundancy == null) {
                Message<String> removeRedundancyBroadcastMsg = new Message<>(
                        MessageType.REMOVE_REDUNDANCY_BROADCAST,
                        node.getNodeId(),
                        "Super Node",
                        geohash5,
                        System.currentTimeMillis()
                );

                SuperNodeTable.getInstance().getAllSuperNodeEntries().stream()
                        .filter(entry -> !entry.getNodeId().equals(node.getNodeId()))           // 자기 자신 제외
                        .forEach(entry -> sender.sendMessage(entry, removeRedundancyBroadcastMsg));

                node.getRoutingTable().removeEntry(redundancyEntry.getNodeId());
                redundancyEntry.setRole(NodeRole.PEER);
                node.getRoutingTable().addEntry(redundancyEntry);
                node.getRoutingTable().setRedundancyEntry(null);

                return;
            }

            nextRedundancy.setRole(NodeRole.REDUNDANCY);

            SuperNodeTable.getInstance().removeRedundancy(geohash5);
            SuperNodeTable.getInstance().addRedundancy(nextRedundancy);

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
                    .filter(entry -> !entry.getNodeId().equals(node.getNodeId()))                           // 자기 자신 제외
                    .forEach(entry -> sender.sendMessage(entry, redundancyBroadcastMsg));

            node.getRoutingTable().getEntries().stream()
                    .filter(entry -> !entry.getNodeId().equals(redundancyEntry.getNodeId()))                // 죽은 Redundancy 제외
                    .filter(entry -> !entry.getNodeId().equals(node.getNodeId()))                           // 자기 자신 제외
                    .forEach(entry -> sender.sendMessage(entry, redundancyBroadcastMsg));

            //System.out.println("Log - Redundancy Disconnect Handler: Promoted Redundancy Broadcast Done");
        }
    }

    private void handleRemoveRedundancyBroadcast(Message<?> message) {
        String geohash = (String) message.getContent();

        if (node.hasAtLeastRole(NodeRole.SUPERNODE)) {
            SuperNodeTable.getInstance().removeRedundancy(geohash);
            new MessageSender(node).sendMessage(node.getRoutingTable().getRedundancyEntry(), new Message<>(
                    MessageType.REMOVE_REDUNDANCY_BROADCAST,
                    node.getNodeId(),
                    node.getRoutingTable().getRedundancyEntry().getNodeId(),
                    geohash,
                    System.currentTimeMillis()
            ));

            //System.out.println("Log - Remove Redundancy Broadcast Handler: Sent Info to Redundancy");

        }
        else {
            SuperNodeTable.getInstance().removeRedundancy(geohash);
        }

        //System.out.println("Log - Remove Redundancy Broadcast Handler: Table Update Successful");
        //SuperNodeTable.getInstance().printTable();

    }


    // 부트스트랩 노드에서만 동작
    private void handleIntroduce(Message<?> message) {
        if (!node.hasAtLeastRole(NodeRole.BOOTSTRAP)) return;
        MessageSender messageSender = new MessageSender(node);

        RoutingEntry peerEntry = (RoutingEntry) message.getContent();
        String senderId = peerEntry.getNodeId();
        String geohash5 = senderId.split("_")[0];

        RoutingEntry existSuperNode = SuperNodeTable.getInstance().getSuperNode(geohash5);
        RoutingEntry existRedundancy = SuperNodeTable.getInstance().getRedundancyNode(geohash5);

        boolean isRedundancyIntroduced =
                peerEntry.getRole() == NodeRole.REDUNDANCY &&
                        existSuperNode != null &&
                        existSuperNode.getNodeId().equals(node.getNodeId());

        if (isRedundancyIntroduced) {
            node.getRoutingTable().setRedundancyEntry(peerEntry);
            node.getRoutingTable().addEntry(peerEntry);

            Message<Void> connectMsg = new Message<>(
                    MessageType.TCP_CONNECT,
                    node.getNodeId(),
                    peerEntry.getNodeId(),
                    null,
                    System.currentTimeMillis()
            );
            messageSender.sendMessage(peerEntry.getIp(), peerEntry.getPort(), connectMsg);

            JoinResponseContent localRoutingTableContent = new JoinResponseContent(
                    node.getRoutingTable().getSuperNodeEntry(),
                    node.getRoutingTable().getRedundancyEntry(),
                    node.getRoutingTable().getEntries().toArray(new RoutingEntry[0])
            );
            Message<JoinResponseContent> localMsg = new Message<>(
                    MessageType.JOIN_RESPONSE,
                    node.getNodeId(),
                    peerEntry.getNodeId(),
                    localRoutingTableContent,
                    System.currentTimeMillis()
            );
            messageSender.sendMessage(peerEntry.getIp(), peerEntry.getPort(), localMsg);

            return;
        }

        if ((existSuperNode == null) || (existRedundancy == null && !checkAlive(existSuperNode))) {
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
            messageSender.sendMessage(
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
                    .forEach(entry -> messageSender.sendMessage(entry, broadcastMsg));

            new MessageSender(node).sendMessage(node.getRoutingTable().getRedundancyEntry(), broadcastMsg);

        } else {
            // 기존 슈퍼노드 할당
            AssignSuperNodeContent content = new AssignSuperNodeContent(
                    false,
                    existSuperNode,
                    null,
                    null
            );
            messageSender.sendMessage(
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
            //System.out.println("[INFO] " + node.getNodeId() + " ▶ 새로운 슈퍼노드로 승격되었습니다.");
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
                    content.getSuperNodeEntry().getNodeId() + " 할당");
        }
    }

    private void handleNewSuperNodeBroadcast(Message<?> message) {
        RoutingEntry newSuperNode = (RoutingEntry) message.getContent();
        System.out.println("[INFO] " + newSuperNode.getNodeId() + " ▶ 새로운 슈퍼노드 등록");
        SuperNodeTable.getInstance().addSuperNode(newSuperNode);

        //System.out.println("Log - New SuperNode Broadcast Handler: Table Update Successful");
        //SuperNodeTable.getInstance().printTable();

        if (node.hasAtLeastRole(NodeRole.SUPERNODE)) {
            new MessageSender(node).sendMessage(node.getRoutingTable().getRedundancyEntry(), new Message<>(
                    MessageType.UPDATE_SUPERNODE_TABLE_SUPER,
                    node.getNodeId(),
                    node.getRoutingTable().getRedundancyEntry().getNodeId(),
                    newSuperNode,
                    System.currentTimeMillis()
            ));

            //System.out.println("Log - New SuperNode Broadcast Handler: Sent Info To Redundancy");

        }
    }


    private void handleNewRedundancyBroadcast(Message<?> message) {
        RoutingEntry redundancyEntry = (RoutingEntry) message.getContent();
        System.out.println("[INFO] " + redundancyEntry.getNodeId() + " ▶ 새로운 Redundancy 등록");
        SuperNodeTable.getInstance().addRedundancy(redundancyEntry);

        if (node.hasAtLeastRole(NodeRole.SUPERNODE)) {
            new MessageSender(node).sendMessage(node.getRoutingTable().getRedundancyEntry(), new Message<>(
                    MessageType.UPDATE_SUPERNODE_TABLE_SUPER,
                    node.getNodeId(),
                    node.getRoutingTable().getRedundancyEntry().getNodeId(),
                    redundancyEntry,
                    System.currentTimeMillis()
            ));

            //System.out.println("Log - New SuperNode Broadcast Handler: Sent Info To Redundancy");

        }

        /*String geohash5 = node.getNodeId().split("_")[0];
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
        }*/
    }

    private void handleJoinRequest(Message<?> message) {
        RoutingEntry peerEntry = (RoutingEntry) message.getContent();
        node.getRoutingTable().addEntry(peerEntry);

        boolean isFirstPeer = node.getRoutingTable().getEntries().size() == 2; // 본인+peer

        if (isFirstPeer || node.getRoutingTable().getRedundancyEntry() == null) {
            peerEntry.setRole(NodeRole.REDUNDANCY);
            node.getRoutingTable().setRedundancyEntry(peerEntry);
            node.getRoutingTable().addEntry(peerEntry);
            SuperNodeTable.getInstance().addRedundancy(peerEntry);

            PromotionContent content = new PromotionContent(
                    SuperNodeTable.getInstance().getAllSuperNodeEntries().toArray(new RoutingEntry[0]),
                    SuperNodeTable.getInstance().getAllRedundancyNodeEntries().toArray(new RoutingEntry[0]),
                    FileMetadataTable.getInstance().getMetadataMap()
            );

            Message<PromotionContent> promoteMsg = new Message<>(
                    MessageType.PROMOTE_REDUNDANCY,
                    node.getNodeId(),
                    peerEntry.getNodeId(),
                    content,
                    System.currentTimeMillis()
            );
            new MessageSender(node).sendMessage(peerEntry.getIp(), peerEntry.getPort(), promoteMsg);

            new MessageSender(node).sendMessage(peerEntry, new Message<>(
                    MessageType.TCP_CONNECT,
                    node.getNodeId(),
                    peerEntry.getNodeId(),
                    null,
                    System.currentTimeMillis()
            ));

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
                        new MessageSender(node).sendMessage(entry, broadcastMsg);
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
                    new MessageSender(node).sendMessage(entry, broadcastMsg);
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
        //node.getRoutingTable().printTable();
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
        if (content.getMetadataMap() != null) {
            FileMetadataTable.getInstance().setMetadataMap(content.getMetadataMap());
            //FileMetadataTable.getInstance().printTable();
        }
        node.promoteToRedundancy();
        SuperNodeTable.getInstance().addRedundancy(new RoutingEntry(node.getNodeId(), node.getIp(), node.getPort(), node.getRole()));

        System.out.println("[INFO] " + node.getNodeId() + " ▶ REDUNDANCY로 승격");
        System.out.println("SupernodeTable/MetadataTable 동기화 완료");
        //SuperNodeTable.getInstance().printTable();
        //FileMetadataTable.getInstance().printTable();
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

    private void handleRequestFileMetadata(Message<?> message) {
        String fileHash = (String) message.getContent();

        String requestId = message.getSenderId();
        RoutingEntry reqEntry = node.getRoutingTable().getEntry(requestId);

        RoutingEntry fileEntry = FileMetadataTable.getInstance().getAnyFileEntry(fileHash);

        if(fileEntry==null) {
            Message<FileMetadataContent> request = new Message<>(
                    MessageType.RESPOND_FILE_METADATA,
                    node.getNodeId(),
                    reqEntry.getNodeId(),
                    new FileMetadataContent(false, fileHash, null),
                    System.currentTimeMillis()
            );
            node.getMessageSender().sendMessage(reqEntry, request);
            return;
        }

        Message<FileMetadataContent> request = new Message<>(
                MessageType.RESPOND_FILE_METADATA,
                node.getNodeId(),
                reqEntry.getNodeId(),
                new FileMetadataContent(true, fileHash, fileEntry),
                System.currentTimeMillis()
        );
        node.getMessageSender().sendMessage(reqEntry, request);
    }

    private void handleResponseFileMetadata(Message<?> message) {
        FileMetadataContent content = (FileMetadataContent) message.getContent();
        String fileHash = content.getFileHash();
        RoutingEntry targetEntry = content.getTargetEntry();

        if(content.isFound()){
            Message<String> request = new Message<>(
                    MessageType.REQUEST_FILE_DOWNLOAD,
                    node.getNodeId(),
                    targetEntry.getNodeId(),
                    fileHash,
                    System.currentTimeMillis()
            );
            node.getMessageSender().sendMessage(targetEntry, request);
            return;
        }

        System.out.println("[INFO] Origin 서버로부터 다운로드 완료");

        RoutingEntry superNodeEntry = node.getRoutingTable().getSuperNodeEntry();

        Message<String> notify = new Message<>(
                MessageType.NOTIFY_FILE_DOWNLOAD,
                node.getNodeId(),
                superNodeEntry.getNodeId(),
                fileHash,
                System.currentTimeMillis()
        );
        node.getMessageSender().sendMessage(superNodeEntry, notify);
    }

    private void handleRequestFileDownload(Message<?> message) {
        String fileHash = (String) message.getContent();

        String requestId = message.getSenderId();
        RoutingEntry reqEntry = node.getRoutingTable().getEntry(requestId);

        Message<FileDownloadContent> request = new Message<>(
                MessageType.RESPOND_FILE_DOWNLOAD,
                node.getNodeId(),
                reqEntry.getNodeId(),
                new FileDownloadContent(true, fileHash, "File Data Content"),
                System.currentTimeMillis()
        );
        node.getMessageSender().sendMessage(reqEntry, request);
    }

    private void handleResponseFileDownload(Message<?> message) {
        FileDownloadContent content = (FileDownloadContent) message.getContent();
        String fileHash = content.getFileHash();
        String fileData = content.getFileData();

        System.out.println("[INFO] 그룹 내 노드로부터 다운로드 완료: From-"+message.getSenderId());

        RoutingEntry superNodeEntry = node.getRoutingTable().getSuperNodeEntry();

        Message<String> request = new Message<>(
                MessageType.NOTIFY_FILE_DOWNLOAD,
                node.getNodeId(),
                superNodeEntry.getNodeId(),
                fileHash,
                System.currentTimeMillis()
        );
        node.getMessageSender().sendMessage(superNodeEntry, request);
    }

    private void handleNotifyFileDownload(Message<?> message) {
        String fileHash = (String) message.getContent();

        String requestId = message.getSenderId();
        RoutingEntry reqEntry = node.getRoutingTable().getEntry(requestId);

        FileMetadataTable.getInstance().addFileMetadata(fileHash, reqEntry);

        RoutingEntry redundancyEntry = node.getRoutingTable().getRedundancyEntry();

        SyncFileMetadataContent content = new SyncFileMetadataContent(fileHash, reqEntry);

        Message<SyncFileMetadataContent> syncMessage = new Message<>(
                MessageType.SYNC_FILE_METADATA,
                node.getNodeId(),
                redundancyEntry.getNodeId(),
                content,
                System.currentTimeMillis()
        );

        node.getMessageSender().sendMessage(redundancyEntry, syncMessage);

        //FileMetadataTable.getInstance().printTable();
    }

    private void handleSyncFileMetadata(Message<?> message) {
        SyncFileMetadataContent content = (SyncFileMetadataContent) message.getContent();
        String fileHash = content.getFileHash();
        RoutingEntry entry = content.getEntry();

        FileMetadataTable.getInstance().addFileMetadata(fileHash, entry);

        //FileMetadataTable.getInstance().printTable();
    }
}
