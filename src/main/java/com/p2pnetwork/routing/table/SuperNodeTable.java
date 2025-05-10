package com.p2pnetwork.routing.table;

import com.p2pnetwork.node.NodeRole;
import com.p2pnetwork.routing.RoutingEntry;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SuperNodeTable {
    private final Map<String, RoutingEntry> superNodeMap = new ConcurrentHashMap<>();

    public void addEntry(RoutingEntry entry) {
        if(!entry.getRole().isAtLeast(NodeRole.SUPERNODE)){
            System.out.println("RoutingEntry가 SUPERNODE 미만의 ROLE을 가짐.");
            return;
        }
        superNodeMap.put(entry.getNodeId(), entry);
        System.out.println("라우팅 테이블에 추가됨: " + entry.getNodeId());
    }

    public Collection<RoutingEntry> getEntries() {
        return superNodeMap.values();
    }
}
