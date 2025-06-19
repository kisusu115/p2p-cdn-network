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

    public RoutingEntry getEntry(String nodeId) {
        return localRoutingMap.get(nodeId);
    }

    public void removeEntry(String nodeId) {
        localRoutingMap.remove(nodeId);
    }

    public Collection<RoutingEntry> getEntries() {
        return localRoutingMap.values();
    }

    public void replaceEntries(Collection<RoutingEntry> entries) {
        localRoutingMap.clear(); // 기존 엔트리 전체 삭제
        for (RoutingEntry entry : entries) {
            localRoutingMap.put(entry.getNodeId(), entry); // 새 엔트리로 교체
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

    public void printTable() {
        System.out.println("==== [LocalRoutingTable] ====");
        String supNodeId = superNodeEntry != null ? superNodeEntry.getNodeId() : "X";
        System.out.println("[슈퍼노드] : "+supNodeId);
        String redNodeId = redundancyEntry != null ? redundancyEntry.getNodeId() : "X";
        System.out.println("[레둔던시] : "+redNodeId);
        System.out.println("[전체 노드]");
        localRoutingMap.forEach((geohash, entry) ->
                System.out.println("  " + entry.getNodeId() + " : (" + entry.getRole() + ")")
        );
        System.out.println("=============================");
    }
}
