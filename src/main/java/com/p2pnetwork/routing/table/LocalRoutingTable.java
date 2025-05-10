package com.p2pnetwork.routing.table;

import com.p2pnetwork.node.Node;
import com.p2pnetwork.routing.RoutingEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LocalRoutingTable {
    private final String selfId;
    private final Map<String, RoutingEntry> localRoutingMap = new ConcurrentHashMap<>();

    public LocalRoutingTable(Node node) {
        this.selfId = node.getNodeId();
    }

    public void addEntry(RoutingEntry entry) {
        localRoutingMap.put(entry.getNodeId(), entry);
    }

    public Collection<RoutingEntry> getEntries() {
        return localRoutingMap.values();
    }

    public void mergeTable(LocalRoutingTable other) {
        for (RoutingEntry entry : other.getEntries()) {
            if (!entry.getNodeId().equals(selfId)) {
                localRoutingMap.putIfAbsent(entry.getNodeId(), entry);
            }
        }
    }
}
