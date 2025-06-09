package com.p2pnetwork.command;

import com.p2pnetwork.message.Message;
import com.p2pnetwork.message.MessageType;
import com.p2pnetwork.message.dto.SyncFileMetadataContent;
import com.p2pnetwork.node.Node;
import com.p2pnetwork.routing.RoutingEntry;
import com.p2pnetwork.routing.file.FileMetadataTable;
import com.p2pnetwork.util.FileKeyGenerator;

import java.util.Scanner;

public class UserCommandHandler implements Runnable {
    private final Node node;

    public UserCommandHandler(Node node) {
        this.node = node;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("[COMMAND] 명령어를 입력하세요.");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            String[] parts = input.trim().split("\\s+");

            if (parts.length == 0) continue;

            String command = parts[0].toLowerCase();

            switch (command) {
                case "download":
                    if (parts.length < 2) {
                        System.out.println("[ERROR] 파일 이름을 입력해주세요.");
                        break;
                    }
                    String fileName = parts[1];
                    handleDownload(fileName);
                    break;

                case "routing":
                    node.getRoutingTable().printTable();
                    break;

                case "exit":
                    System.out.println("[INFO] 프로그램을 종료합니다.");
                    System.exit(0);
                    break;

                default:
                    System.out.println("[ERROR] 알 수 없는 명령어입니다: " + command);
            }
        }
    }

    private void handleDownload(String fileName) {
        String fileHash = FileKeyGenerator.generateFileKey(fileName);
        System.out.println("[USER] '" + fileHash + "' 다운로드 요청");

        if (node.iAmSuperNode()) {
            RoutingEntry targetEntry = FileMetadataTable.getInstance().getAnyFileEntry(fileHash);

            if (targetEntry == null) {
                FileMetadataTable.getInstance().addFileMetadata(fileHash, RoutingEntry.from(node));

                RoutingEntry redundancyEntry = node.getRoutingTable().getRedundancyEntry();

                SyncFileMetadataContent content = new SyncFileMetadataContent(fileHash, RoutingEntry.from(node));

                Message<SyncFileMetadataContent> syncMessage = new Message<>(
                        MessageType.SYNC_FILE_METADATA,
                        node.getNodeId(),
                        redundancyEntry.getNodeId(),
                        content,
                        System.currentTimeMillis()
                );

                node.getMessageSender().sendMessage(redundancyEntry, syncMessage);

                FileMetadataTable.getInstance().printTable();
                return;
            }

            requestDownload(fileHash, targetEntry);
        } else {
            RoutingEntry superNodeEntry = node.getRoutingTable().getSuperNodeEntry();
            requestMetadata(fileHash, superNodeEntry);
        }
    }

    private void requestMetadata(String fileHash, RoutingEntry superNodeEntry){
        Message<String> request = new Message<>(
                MessageType.REQUEST_FILE_METADATA,
                node.getNodeId(),
                superNodeEntry.getNodeId(),
                fileHash,
                System.currentTimeMillis()
        );
        node.getMessageSender().sendMessage(superNodeEntry, request);
    }

    private void requestDownload(String fileHash, RoutingEntry targetEntry){
        Message<String> request = new Message<>(
                MessageType.REQUEST_FILE_DOWNLOAD,
                node.getNodeId(),
                targetEntry.getNodeId(),
                fileHash,
                System.currentTimeMillis()
        );
        node.getMessageSender().sendMessage(targetEntry, request);
    }
}
