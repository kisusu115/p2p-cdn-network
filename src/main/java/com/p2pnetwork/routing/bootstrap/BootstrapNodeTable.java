package com.p2pnetwork.routing.bootstrap;

import com.p2pnetwork.node.NodeRole;
import com.p2pnetwork.routing.RoutingEntry;
import com.p2pnetwork.util.NodeIdGenerator;
import java.util.*;

public class BootstrapNodeTable {
    public static final Map<String, RoutingEntry> staticBootstrapMap = new HashMap<>();
    static {
        try {
            // 서울
            String idSeoul = NodeIdGenerator.generateNodeId(37.5665, 126.9780, "127.0.0.1", 10001);
            staticBootstrapMap.put(idSeoul, new RoutingEntry(
                    idSeoul, "127.0.0.1", 10001,
                    NodeRole.SUPERNODE
            ));
            // 뉴욕
            String idNY = NodeIdGenerator.generateNodeId(40.7128, -74.0060, "127.0.0.1", 10002);
            staticBootstrapMap.put(idNY, new RoutingEntry(
                    idNY, "127.0.0.1", 10002,
                    NodeRole.SUPERNODE
            ));
            // 런던
            String idLondon = NodeIdGenerator.generateNodeId(51.5074, -0.1278, "127.0.0.1", 10003);
            staticBootstrapMap.put(idLondon, new RoutingEntry(
                    idLondon, "127.0.0.1", 10003,
                    NodeRole.SUPERNODE
            ));
            // 시드니
            String idSydney = NodeIdGenerator.generateNodeId(-33.8688, 151.2093, "127.0.0.1", 10004);
            staticBootstrapMap.put(idSydney, new RoutingEntry(
                    idSydney, "127.0.0.1", 10004,
                    NodeRole.SUPERNODE
            ));
        } catch (Exception e) {
            throw new RuntimeException("부트스트랩 노드 초기화 실패", e);
        }
    }

    public static boolean isBootstrapPort(int port) {
        return staticBootstrapMap.values().stream().anyMatch(entry -> entry.getPort() == port);
    }

    public static RoutingEntry getBootstrapByPort(int port) {
        return staticBootstrapMap.values().stream().filter(e -> e.getPort() == port).findFirst().orElse(null);
    }

    public static Collection<RoutingEntry> getAll() { return staticBootstrapMap.values(); }
}
