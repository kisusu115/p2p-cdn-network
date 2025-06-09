package com.p2pnetwork.routing.file;

import com.p2pnetwork.routing.RoutingEntry;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FileMetadataTable {
    private static final FileMetadataTable INSTANCE = new FileMetadataTable();
    private Map<String, Set<RoutingEntry>> metadataMap = new ConcurrentHashMap<>();

    public static FileMetadataTable getInstance() {
        return INSTANCE;
    }

    public synchronized void addFileMetadata(String fileHash, RoutingEntry entry) {
        Set<RoutingEntry> routingEntrySetSet = metadataMap.get(fileHash);

        if (routingEntrySetSet == null) {
            routingEntrySetSet = new HashSet<>();
            routingEntrySetSet.add(entry);
            metadataMap.put(fileHash, routingEntrySetSet);
            return;
        }

        routingEntrySetSet.add(entry);
    }


    public synchronized Map<String, Set<RoutingEntry>> getMetadataMap() {
        Map<String, Set<RoutingEntry>> copy = new ConcurrentHashMap<>();
        for (Map.Entry<String, Set<RoutingEntry>> entry : metadataMap.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
    }

    public synchronized void setMetadataMap(Map<String, Set<RoutingEntry>> newMap) {
        Map<String, Set<RoutingEntry>> copy = new ConcurrentHashMap<>();
        for (Map.Entry<String, Set<RoutingEntry>> entry : newMap.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        this.metadataMap = copy;
    }

    public synchronized RoutingEntry getAnyFileEntry(String fileHash) {
        Set<RoutingEntry> entries = metadataMap.get(fileHash);
        if (entries == null || entries.isEmpty()) return null;
        return entries.iterator().next();
    }

    public void printTable() {
        System.out.println("==== [FileMetadataTable] ====");
        if (metadataMap == null || metadataMap.isEmpty()) {
            System.out.println("     (메타데이터 없음)");
        } else {
            metadataMap.forEach((fileKey, entries) -> {
                System.out.println("  " + fileKey + " : " + entries.size() + " node(s)");
                entries.forEach(entry -> System.out.println("    - " + entry.getNodeId()));
            });
        }
        System.out.println("=============================");
    }
}