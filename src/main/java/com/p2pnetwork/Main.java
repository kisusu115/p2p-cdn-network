package com.p2pnetwork;

import com.p2pnetwork.message.Message;
import com.p2pnetwork.message.MessageType;
import com.p2pnetwork.node.Node;

import java.util.Map;

public class Main {
    // 부트스트랩 노드 사전 정의 포트 및 좌표
    private static final Map<Integer, double[]> BOOTSTRAP_NODES = Map.of(
            10001, new double[]{37.5665, 126.9780}, // 서울
            10002, new double[]{40.7128, -74.0060},     // 뉴욕
            10003, new double[]{51.5074, -0.1278},      // 런던
            10004, new double[]{-33.8688, 151.2093}     // 시드니
    );

    // 대응되는 레둔던시 포트 → Bootstrap 좌표
    private static final Map<Integer, double[]> REDUNDANCY_NODES = Map.of(
            10005, BOOTSTRAP_NODES.get(10001),      // 서울
            10006, BOOTSTRAP_NODES.get(10002),          // 뉴욕
            10007, BOOTSTRAP_NODES.get(10003),          // 런던
            10008, BOOTSTRAP_NODES.get(10004)           // 시드니
    );

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                printUsage();
                System.exit(1);
            }

            String role = args[0].toLowerCase();
            switch (role) {
                case "bootstrap":
                    int port = Integer.parseInt(args[1]);
                    startBootstrapNode(port);
                    break;

                case "redundancy":
                    int redPort = Integer.parseInt(args[1]);
                    startRedundancyNode(redPort);
                    break;

                case "revive":
                    int revPort = Integer.parseInt(args[1]);
                    reviveBootstrapNode(revPort);
                    break;

                case "redrevive":
                    int redrevPort = Integer.parseInt(args[1]);
                    reviveRedundancyNode(redrevPort);
                    break;

                case "node":
                    if (args.length < 4) {
                        printUsage();
                        System.exit(1);
                    }
                    double lat = Double.parseDouble(args[1]);
                    double lon = Double.parseDouble(args[2]);
                    int nodePort = Integer.parseInt(args[3]);
                    startPeerNode(lat, lon, nodePort);
                    break;

                default:
                    printUsage();
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void startBootstrapNode(int port) throws Exception {
        double[] coordinates = BOOTSTRAP_NODES.get(port);
        if (coordinates == null) {
            System.err.println("[ERROR] 지원하지 않는 부트스트랩 포트입니다.");
            printUsage();
            System.exit(1);
        }
        Node node = new Node(coordinates[0], coordinates[1], port);
        node.start();
        System.out.println("[INFO] 부트스트랩 노드 시작: " + node.getNodeId());
    }

    private static void startRedundancyNode(int port) throws Exception {
        double[] coordinates = REDUNDANCY_NODES.get(port);
        if (coordinates == null) {
            System.err.println("[ERROR] 지원하지 않는 레둔던시 포트입니다.");
            printUsage();
            System.exit(1);
        }
        Node node = new Node(coordinates[0], coordinates[1], port);
        node.start();
        System.out.println("[INFO] 레둔던시 노드 시작: " + node.getNodeId());
    }

    private static void reviveBootstrapNode(int port) throws Exception {
        double[] coordinates = BOOTSTRAP_NODES.get(port);
        if (coordinates == null) {
            System.err.println("[ERROR] 지원하지 않는 부트스트랩 포트입니다.");
            printUsage();
            System.exit(1);
        }
        Node node = new Node(coordinates[0], coordinates[1], port);
        node.start();
        node.bootstrapRevival();
        System.out.println("[INFO] 부트스트랩 노드 복구됨: " + node.getNodeId());
    }

    private static void reviveRedundancyNode(int port) throws Exception {
        double[] coordinates = REDUNDANCY_NODES.get(port);
        if (coordinates == null) {
            System.err.println("[ERROR] 지원하지 않는 레둔던시 포트입니다.");
            printUsage();
            System.exit(1);
        }
        Node node = new Node(coordinates[0], coordinates[1], port);
        node.start();
        node.redundancyRevival();
        System.out.println("[INFO] 레둔던시 노드 복구됨: " + node.getNodeId());
    }

    private static void startPeerNode(double lat, double lon, int port) throws Exception {
        validateCoordinates(lat, lon);
        Node node = new Node(lat, lon, port);
        node.start();
        System.out.println("[INFO] 일반 노드 시작: " + node.getNodeId());
    }

    private static void printUsage() {
        System.out.println("[INFO] Usage:");
        System.out.println("[INFO] 부트스트랩 노드: java -jar p2p-net.jar bootstrap <port(10001~10004)>");
        System.out.println("[INFO] 레둔던시 노드: java -jar p2p-net.jar redundancy <port(10005~10008)>");
        System.out.println("[INFO] 일반 노드: java -jar p2p-net.jar node <latitude> <longitude> <port>");
    }

    private static void validateCoordinates(double lat, double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("[ERROR] 잘못된 좌표값. 위도: [-90~90], 경도: [-180~180]");
        }
    }
}
