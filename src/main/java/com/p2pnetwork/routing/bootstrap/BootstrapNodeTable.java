package com.p2pnetwork.routing.bootstrap;

import com.p2pnetwork.routing.RoutingEntry;

import java.util.HashMap;
import java.util.Map;

import static com.p2pnetwork.node.NodeRole.BOOTSTRAP;

public class BootstrapNodeTable {
    public static final Map<String, RoutingEntry> staticBootstrapMap = new HashMap<>();

    static {
        staticBootstrapMap.put("S01", new RoutingEntry("S01", "127.0.0.1", 10001, BOOTSTRAP));
        staticBootstrapMap.put("S02", new RoutingEntry("S02", "127.0.0.1", 10002, BOOTSTRAP));
        staticBootstrapMap.put("S03", new RoutingEntry("S03", "127.0.0.1", 10003, BOOTSTRAP));
        staticBootstrapMap.put("S04", new RoutingEntry("S04", "127.0.0.1", 10004, BOOTSTRAP));
    }

    public static boolean isPortBootstrapNode(int port) {
        return staticBootstrapMap.values().stream()
                .anyMatch(entry -> entry.getPort() == port);
    }
}
