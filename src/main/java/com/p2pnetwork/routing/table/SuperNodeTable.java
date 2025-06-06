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

    public synchronized RoutingEntry getRedundancyNode(String geohash5) {
        return redundancyMap.get(geohash5);
    }

    public synchronized Collection<RoutingEntry> getAllRedundancyNodeEntries() {
        return redundancyMap.values();
    }

    public synchronized void removeSuperNode(String geohash5) {
        superNodeMap.remove(geohash5);
    }

    public synchronized void removeSuperNode(RoutingEntry entry) {
        String geohash = entry.getNodeId().split("_")[0];
        removeSuperNode(geohash);
    }

    public synchronized void removeRedundancy(String geohash5) {
        redundancyMap.remove(geohash5);
    }

    public synchronized void removeRedundancy(RoutingEntry entry) {
        String geohash = entry.getNodeId().split("_")[0];
        removeRedundancy(geohash);
    }

    public synchronized void exchangeBootstrap(String geohash5){
        RoutingEntry redundancyEntry = redundancyMap.get(geohash5);
        redundancyMap.put(geohash5, superNodeMap.get(geohash5));
        superNodeMap.put(geohash5, redundancyEntry);
    }

    public void clear() {
        superNodeMap.clear();
        redundancyMap.clear();
    }

    public void printTable() {
        System.out.println("==== [SuperNodeTable] ====");
        System.out.println("[슈퍼노드]");
        superNodeMap.forEach((geohash, entry) ->
                System.out.println("  " + geohash + " : " + entry.getNodeId() + " (" + entry.getRole() + ")")
        );
        System.out.println("[레둔던시]");
        redundancyMap.forEach((geohash, entry) ->
                System.out.println("  " + geohash + " : " + entry.getNodeId() + " (" + entry.getRole() + ")")
        );
        System.out.println("==========================");
    }
}
