package com.p2pnetwork.routing.table;

import com.p2pnetwork.routing.RoutingEntry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Collection;

public class SuperNodeTable {
    private static final SuperNodeTable INSTANCE = new SuperNodeTable();
    private final Map<String, RoutingEntry> superNodeMap = new ConcurrentHashMap<>();   // 5자리 geohash → 슈퍼노드 1개
    private final Map<String, RoutingEntry> redundancyMap = new ConcurrentHashMap<>();  // 위와 동일, 격자당 레둔던시 1개

    public static SuperNodeTable getInstance() { return INSTANCE; }

    public synchronized void addSuperNode(RoutingEntry entry) {
        String geohash = entry.getNodeId().split("_")[0];
        superNodeMap.put(geohash, entry);
    }

    public synchronized RoutingEntry getSuperNode(String geohash5) {
        return superNodeMap.get(geohash5);
    }

    public synchronized Collection<RoutingEntry> getAllSuperNodeEntries() {
        return superNodeMap.values();
    }

    public synchronized void addRedundancy(RoutingEntry entry) {
        String geohash = entry.getNodeId().split("_")[0];
        redundancyMap.put(geohash, entry);
    }

    public synchronized void removeSuperNode(String geohash5) {
        superNodeMap.remove(geohash5);
    }

    public synchronized void removeSuperNode(RoutingEntry entry) {
        String geohash = entry.getNodeId().split("_")[0];
        removeSuperNode(geohash);
    }
}
