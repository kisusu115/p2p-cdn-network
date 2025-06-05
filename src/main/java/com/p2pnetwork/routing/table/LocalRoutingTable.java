package com.p2pnetwork.routing.table;

import com.p2pnetwork.node.Node;
import com.p2pnetwork.routing.RoutingEntry;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter @Setter
public class LocalRoutingTable {
    private final String selfId;
    private RoutingEntry superNodeEntry;
    private RoutingEntry redundancyEntry;
    private final Map<String, RoutingEntry> localRoutingMap = new ConcurrentHashMap<>();

    public LocalRoutingTable(Node node) {
        this.selfId = node.getNodeId();
    }

    public void addEntry(RoutingEntry entry) {
        localRoutingMap.put(entry.getNodeId(), entry);
    }

    public void removeEntry(String nodeId) {
        localRoutingMap.remove(nodeId);
    }

    public Collection<RoutingEntry> getEntries() {
        return localRoutingMap.values();
    }

    public void mergeEntries(Collection<RoutingEntry> entries) {
        for (RoutingEntry entry : entries) {
            if (!entry.getNodeId().equals(selfId)) {
                localRoutingMap.putIfAbsent(entry.getNodeId(), entry);
            }
        }
    }

    public void setSuperNodeEntry(RoutingEntry entry) {
        this.superNodeEntry = entry;
        addEntry(entry);
    }

    public synchronized void exchangeBootstrap() {
        RoutingEntry tempEntry = this.redundancyEntry;
        this.redundancyEntry = this.superNodeEntry;
        this.superNodeEntry = tempEntry;
    }
}
