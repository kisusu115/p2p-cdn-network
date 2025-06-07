package com.p2pnetwork.routing.bootstrap;

import com.p2pnetwork.node.NodeRole;
import com.p2pnetwork.routing.RoutingEntry;
import com.p2pnetwork.routing.table.SuperNodeTable;
import com.p2pnetwork.util.NodeIdGenerator;

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BootstrapNodeTable {
    public static final Map<String, RoutingEntry> staticBootstrapMap = new HashMap<>();

    static {
        try {
            String localIp = InetAddress.getLocalHost().getHostAddress();;

            // 서울
            String seoulId = NodeIdGenerator.generateNodeId(37.5665, 126.9780, localIp, 10001);
            String seoulGeohash = seoulId.split("_")[0];
            RoutingEntry seoulSuper = new RoutingEntry(seoulId, localIp, 10001, NodeRole.SUPERNODE);
            staticBootstrapMap.put(seoulGeohash, seoulSuper);
            SuperNodeTable.getInstance().addSuperNode(seoulSuper);

            String seoulRedId = NodeIdGenerator.generateNodeId(37.5665, 126.9780, localIp, 10005);
            RoutingEntry seoulRed = new RoutingEntry(seoulRedId, localIp, 10005, NodeRole.REDUNDANCY);
            SuperNodeTable.getInstance().addRedundancy(seoulRed);

            // 뉴욕
            String nyId = NodeIdGenerator.generateNodeId(40.7128, -74.0060, localIp, 10002);
            String nyGeohash = nyId.split("_")[0];
            RoutingEntry nySuper = new RoutingEntry(nyId, localIp, 10002, NodeRole.SUPERNODE);
            staticBootstrapMap.put(nyGeohash, nySuper);
            SuperNodeTable.getInstance().addSuperNode(nySuper);

            String nyRedId = NodeIdGenerator.generateNodeId(40.7128, -74.0060, localIp, 10006);
            RoutingEntry nyRed = new RoutingEntry(nyRedId, localIp, 10006, NodeRole.REDUNDANCY);
            SuperNodeTable.getInstance().addRedundancy(nyRed);

            // 런던
            String londonId = NodeIdGenerator.generateNodeId(51.5074, -0.1278, localIp, 10003);
            String londonGeohash = londonId.split("_")[0];
            RoutingEntry londonSuper = new RoutingEntry(londonId, localIp, 10003, NodeRole.SUPERNODE);
            staticBootstrapMap.put(londonGeohash, londonSuper);
            SuperNodeTable.getInstance().addSuperNode(londonSuper);

            String londonRedId = NodeIdGenerator.generateNodeId(51.5074, -0.1278, localIp, 10007);
            RoutingEntry londonRed = new RoutingEntry(londonRedId, localIp, 10007, NodeRole.REDUNDANCY);
            SuperNodeTable.getInstance().addRedundancy(londonRed);

            // 시드니
            String sydneyId = NodeIdGenerator.generateNodeId(-33.8688, 151.2093, localIp, 10004);
            String sydneyGeohash = sydneyId.split("_")[0];
            RoutingEntry sydneySuper = new RoutingEntry(sydneyId, localIp, 10004, NodeRole.SUPERNODE);
            staticBootstrapMap.put(sydneyGeohash, sydneySuper);
            SuperNodeTable.getInstance().addSuperNode(sydneySuper);

            String sydneyRedId = NodeIdGenerator.generateNodeId(-33.8688, 151.2093, localIp, 10008);
            RoutingEntry sydneyRed = new RoutingEntry(sydneyRedId, localIp, 10008, NodeRole.REDUNDANCY);
            SuperNodeTable.getInstance().addRedundancy(sydneyRed);

        } catch (Exception e) {
            throw new RuntimeException("부트스트랩 노드 초기화 실패", e);
        }
    }

    public static RoutingEntry getBootstrapByGeohash(String geohash5) {
        return staticBootstrapMap.get(geohash5);
    }

    public static Collection<RoutingEntry> getAll() {
        return staticBootstrapMap.values();
    }
}
